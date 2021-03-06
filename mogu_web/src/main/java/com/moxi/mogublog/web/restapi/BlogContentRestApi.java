package com.moxi.mogublog.web.restapi;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxi.mogublog.commons.entity.Blog;
import com.moxi.mogublog.commons.entity.SystemConfig;
import com.moxi.mogublog.commons.feign.PictureFeignClient;
import com.moxi.mogublog.utils.IpUtils;
import com.moxi.mogublog.utils.ResultUtil;
import com.moxi.mogublog.utils.StringUtils;
import com.moxi.mogublog.web.annotion.log.BussinessLog;
import com.moxi.mogublog.web.global.MessageConf;
import com.moxi.mogublog.web.global.SysConf;
import com.moxi.mogublog.xo.global.RedisConf;
import com.moxi.mogublog.xo.service.BlogService;
import com.moxi.mogublog.xo.service.SystemConfigService;
import com.moxi.mogublog.xo.utils.WebUtil;
import com.moxi.mougblog.base.enums.EBehavior;
import com.moxi.mougblog.base.enums.EOpenStatus;
import com.moxi.mougblog.base.enums.EPublish;
import com.moxi.mougblog.base.enums.EStatus;
import com.moxi.mougblog.base.global.BaseSysConf;
import com.moxi.mougblog.base.global.Constants;
import com.moxi.mougblog.base.global.ECode;
import com.moxi.mougblog.base.holder.RequestHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ???????????? RestApi
 *
 * @author ??????
 * @date 2018-09-04
 */
@RestController
@RefreshScope
@RequestMapping("/content")
@Api(value = "????????????????????????", tags = {"????????????????????????"})
@Slf4j
public class BlogContentRestApi {
    @Autowired
    private WebUtil webUtil;
    @Autowired
    private BlogService blogService;
    @Resource
    private PictureFeignClient pictureFeignClient;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value(value = "${BLOG.ORIGINAL_TEMPLATE}")
    private String ORIGINAL_TEMPLATE;
    @Value(value = "${BLOG.REPRINTED_TEMPLATE}")
    private String REPRINTED_TEMPLATE;

    @BussinessLog(value = "????????????", behavior = EBehavior.BLOG_CONTNET)
    @ApiOperation(value = "??????Uid??????????????????", notes = "??????Uid??????????????????")
    @GetMapping("/getBlogByUid")
    public String getBlogByUid(@ApiParam(name = "uid", value = "??????UID", required = false) @RequestParam(name = "uid", required = false) String uid,
                               @ApiParam(name = "oid", value = "??????OID", required = false) @RequestParam(name = "oid", required = false, defaultValue = "0") Integer oid) {

        HttpServletRequest request = RequestHolder.getRequest();
        String ip = IpUtils.getIpAddr(request);
        if (StringUtils.isEmpty(uid) && oid == 0) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.PARAM_INCORRECT);
        }
        Blog blog = null;
        if (StringUtils.isNotEmpty(uid)) {
            blog = blogService.getById(uid);
        } else {
            QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq(SysConf.OID, oid);
            queryWrapper.last(SysConf.LIMIT_ONE);
            blog = blogService.getOne(queryWrapper);
        }

        // ?????????????????????????????????,?????????????????????????????????????????????
        Object attribute = request.getAttribute(SysConf.USER_TAG);
        Integer userTag = null;
        if (attribute != null) {
            userTag = ((Double) attribute).intValue();
        }
        // ?????????????????????????????????
        SystemConfig systemConfig = systemConfigService.getConfig();
        String openBlogFilter = systemConfig.getOpenBlogFilter();
        if (EOpenStatus.CLOSE.equals(openBlogFilter)) {
            if (blog == null || blog.getStatus() == EStatus.DISABLED || EPublish.NO_PUBLISH.equals(blog.getIsPublish())) {
                return ResultUtil.result(ECode.ERROR, MessageConf.BLOG_IS_DELETE);
            }
        } else {
            if (blog == null || blog.getStatus() == EStatus.DISABLED || (EPublish.NO_PUBLISH.equals(blog.getIsPublish()) && (userTag == null || userTag.equals(BaseSysConf.ZERO)))) {
                return ResultUtil.result(ECode.ERROR, MessageConf.BLOG_IS_DELETE);
            }
        }

        // ????????????????????????
        setBlogCopyright(blog);

        //??????????????????
        blogService.setTagByBlog(blog);

        //????????????
        blogService.setSortByBlog(blog);

        //?????????????????????
        setPhotoListByBlog(blog);

        //???Redis?????????????????????????????????????????????
        String jsonResult = stringRedisTemplate.opsForValue().get("BLOG_CLICK:" + ip + "#" + blog.getUid());

        if (StringUtils.isEmpty(jsonResult)) {

            //????????????????????????
            Integer clickCount = blog.getClickCount() + 1;
            blog.setClickCount(clickCount);
            blog.updateById();

            //?????????????????????????????????redis???, 24???????????????
            stringRedisTemplate.opsForValue().set(RedisConf.BLOG_CLICK + Constants.SYMBOL_COLON + ip + Constants.SYMBOL_WELL + blog.getUid(), blog.getClickCount().toString(),
                    24, TimeUnit.HOURS);
        }
        return ResultUtil.result(SysConf.SUCCESS, blog);
    }

    @ApiOperation(value = "??????Uid?????????????????????", notes = "??????Uid?????????????????????")
    @GetMapping("/getBlogPraiseCountByUid")
    public String getBlogPraiseCountByUid(@ApiParam(name = "uid", value = "??????UID", required = false) @RequestParam(name = "uid", required = false) String uid) {

        return ResultUtil.result(SysConf.SUCCESS, blogService.getBlogPraiseCountByUid(uid));
    }

    @BussinessLog(value = "??????Uid???????????????", behavior = EBehavior.BLOG_PRAISE)
    @ApiOperation(value = "??????Uid???????????????", notes = "??????Uid???????????????")
    @GetMapping("/praiseBlogByUid")
    public String praiseBlogByUid(@ApiParam(name = "uid", value = "??????UID", required = false) @RequestParam(name = "uid", required = false) String uid) {
        if (StringUtils.isEmpty(uid)) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.PARAM_INCORRECT);
        }
        return blogService.praiseBlogByUid(uid);
    }

    @ApiOperation(value = "????????????Uid?????????????????????", notes = "?????????????????????????????????")
    @GetMapping("/getSameBlogByTagUid")
    public String getSameBlogByTagUid(@ApiParam(name = "tagUid", value = "????????????UID", required = true) @RequestParam(name = "tagUid", required = true) String tagUid,
                                      @ApiParam(name = "currentPage", value = "????????????", required = false) @RequestParam(name = "currentPage", required = false, defaultValue = "1") Long currentPage,
                                      @ApiParam(name = "pageSize", value = "??????????????????", required = false) @RequestParam(name = "pageSize", required = false, defaultValue = "10") Long pageSize) {
        if (StringUtils.isEmpty(tagUid)) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.PARAM_INCORRECT);
        }
        return ResultUtil.result(SysConf.SUCCESS, blogService.getSameBlogByTagUid(tagUid));
    }

    @ApiOperation(value = "??????BlogUid?????????????????????", notes = "??????BlogUid?????????????????????")
    @GetMapping("/getSameBlogByBlogUid")
    public String getSameBlogByBlogUid(@ApiParam(name = "blogUid", value = "????????????UID", required = true) @RequestParam(name = "blogUid", required = true) String blogUid) {
        if (StringUtils.isEmpty(blogUid)) {
            return ResultUtil.result(SysConf.ERROR, MessageConf.PARAM_INCORRECT);
        }
        List<Blog> blogList = blogService.getSameBlogByBlogUid(blogUid);
        IPage<Blog> pageList = new Page<>();
        pageList.setRecords(blogList);
        return ResultUtil.result(SysConf.SUCCESS, pageList);
    }

    /**
     * ?????????????????????
     *
     * @param blog
     */
    private void setPhotoListByBlog(Blog blog) {
        //??????????????????
        if (blog != null && !StringUtils.isEmpty(blog.getFileUid())) {
            String result = this.pictureFeignClient.getPicture(blog.getFileUid(), Constants.SYMBOL_COMMA);
            List<String> picList = webUtil.getPicture(result);
            if (picList != null && picList.size() > 0) {
                blog.setPhotoList(picList);
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param blog
     */
    private void setBlogCopyright(Blog blog) {

        //?????????????????????
        if (Constants.STR_ONE.equals(blog.getIsOriginal())) {
            blog.setCopyright(ORIGINAL_TEMPLATE);
        } else {
            String reprintedTemplate = REPRINTED_TEMPLATE;
            String[] variable = {blog.getArticlesPart(), blog.getAuthor()};
            String str = String.format(reprintedTemplate, variable);
            blog.setCopyright(str);
        }
    }
}

