<template>
  <div class="app-container">
    <!-- 查询和其他操作 -->
    <div class="filter-container" style="margin: 10px 0 10px 0;">
      <el-input clearable class="filter-item" style="width: 200px;" v-model="queryParams.content"
                placeholder="请输入评论内容"></el-input>
      <el-input clearable class="filter-item" style="width: 150px;" v-model="queryParams.userName"
                placeholder="请输入评论人"></el-input>

      <el-select v-model="queryParams.type" clearable placeholder="评论类型" style="margin-left:5px;width:110px">
        <el-option
          v-for="item in commentTypeDictList"
          :key="item.uid"
          :label="item.dictLabel"
          :value="item.dictValue"
        ></el-option>
      </el-select>

      <el-select v-model="queryParams.source" clearable placeholder="评论来源" style="margin-left:5px;width:110px">
        <el-option
          v-for="item in commentSourceDictList"
          :key="item.uid"
          :label="item.dictLabel"
          :value="item.dictValue"
        ></el-option>
      </el-select>

      <el-select v-model="queryParams.status" clearable placeholder="是否已删除" style="margin-left:5px;width:120px">
        <el-option
          v-for="item in paramsStatusDictList"
          :key="item.uid"
          :label="item.dictValue == 2 ? '已删除' : item.dictLabel"
          :value="item.dictValue"
        ></el-option>
      </el-select>

      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFind"
                 v-permission="'/comment/getList'">查找
      </el-button>
      <el-button class="filter-item" type="danger" @click="handleDeleteBatch" icon="el-icon-delete"
                 v-permission="'/comment/deleteBatch'">删除选中
      </el-button>
    </div>

    <el-table :data="tableData" style="width: 100%" max-height="530" @selection-change="handleSelectionChange">

      <el-table-column type="selection"></el-table-column>

      <el-table-column label="序号" width="100%" align="center">
        <template slot-scope="scope">
          <span>{{ scope.$index + 1 }}</span>
        </template>
      </el-table-column>

      <el-table-column label="头像" width="100%" align="center">
        <template slot-scope="scope">
          <img
            v-if="scope.row.user"
            :src="scope.row.user.photoUrl"
            onerror="onerror=null;src='https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif'"
            style="width: 100px;height: 100px;"
          >
          <img
            v-else
            src="https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif"
            style="width: 100px;height: 100px;"
          >
        </template>
      </el-table-column>

      <el-table-column label="评论人" width="110" align="center">
        <template slot-scope="scope">
          <el-tag type="primary" v-if="scope.row.user" style="cursor: pointer;" @click.native="goUser(scope.row.user)">
            {{ scope.row.user.nickName }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="被评论人" width="110" align="center">
        <template slot-scope="scope">
          <el-tag type="info" v-if="scope.row.toUser" style="cursor: pointer;" @click.native="goUser(scope.row.toUser)">
            {{ scope.row.toUser.nickName }}
          </el-tag>
          <el-tag type="info" style="cursor: pointer;" v-else>无</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="类型" width="100%" align="center">
        <template slot-scope="scope">
          <template>
            <el-tag type="danger" v-if="scope.row.type == 1">点赞</el-tag>
            <el-tag type="success" v-if="scope.row.type == 0">评论</el-tag>
          </template>
        </template>
      </el-table-column>

      <el-table-column label="来源" width="115" align="center">
        <template slot-scope="scope">
          <template>
            <el-tag type="danger" v-if="scope.row.blog ? scope.row.blog.status == 0 : false" style="cursor: pointer;">
              该博客已删除
            </el-tag>
            <el-tag type="danger" v-else-if="scope.row.blog ? scope.row.blog.isPublish == 0 : false"
                    style="cursor: pointer;"
                    @click.native="goBlog(scope.row.blog)">
              该博客已下架
            </el-tag>
            <el-tag type="warning" v-else @click.native="goPage(scope.row.source, scope.row.blog)"
                    style="cursor: pointer;">
              {{ scope.row.sourceName }}
            </el-tag>
          </template>
        </template>
      </el-table-column>

      <el-table-column label="内容" width="260px" align="center">
        <template slot-scope="scope">
          <span v-html="$xss(scope.row.content, options)"></span>
        </template>
      </el-table-column>

      <el-table-column label="创建时间" width="100%" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="80" align="center">
        <template slot-scope="scope">
          <template>
            <el-tag v-for="item in paramsStatusDictList" :key="item.uid" :type="item.listClass"
                    v-if="scope.row.status == item.dictValue">{{ item.dictValue == 2 ? '已删除' : item.dictLabel }}
            </el-tag>
          </template>
        </template>
      </el-table-column>

      <el-table-column label="操作" fixed="right" min-width="100%" align="center">
        <template slot-scope="scope">
          <el-button @click="handleReply(scope.row)" type="success" size="small">回复</el-button>
          <el-button @click="handleDelete(scope.row)" type="danger" size="small" v-permission="'/comment/delete'">删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!--分页-->
    <div class="block">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        layout="total, prev, pager, next, jumper"
        :total="total">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import {getCommentList, addComment, editComment, deleteComment, deleteBatchComment} from "@/api/comment";
import {getListByDictTypeList} from "@/api/sysDictData"

export default {
  data() {
    return {
      // xss白名单配置
      options: {
        whiteList: {
          a: ['href', 'title', 'target', 'style'],
          span: ['class', 'style']
        }
      },
      queryParams: {
        content: null, //评论名
        userName: null, //用户名
        type: null, //类型
        source: null, //来源
        status: null
      }, //查询参数
      multipleSelection: [], //多选，用于批量删除
      BLOG_WEB_URL: process.env.BLOG_WEB_URL,
      tableData: [],
      keyword: "",
      currentPage: 1,
      pageSize: 10,
      total: 0, //总数量
      title: "增加友链",
      dialogFormVisible: false, //控制弹出框
      formLabelWidth: '120px',
      isEditForm: false,
      commentTypeDictList: [], //评论类型字典
      commentSourceDictList: [], //评论来源字典
      commentTypeDefaultValue: null, // 评论类型默认值
      paramsStatusDictList: [],
      paramsStatusDefault: null,
    };
  },
  created() {
    // 获取评论
    this.commentList();

    // 获取字典
    this.getDictList()
  },
  methods: {
    // 跳转到用户中心
    goUser: function (user) {
      console.log("go user", user)
      this.$router.push({path: "/user/user", query: {nickName: user.nickName}});
    },
    goBlog: function (blog) {
      console.log("go blog", blog)
      this.$router.push({
        path: "/blog/blog", query: {
          keyword: blog.title,
          tag: blog.tagUid,
          blogSort: blog.blogSortUid,
          levelKeyword: blog.level,
          publishKeyword: blog.isPublish,
          originalKeyword: blog.isOriginal,
          typeKeyword: blog.type
        }
      });
    },
    // 跳转到该博客详情
    onClick: function (row) {
      console.log("点击跳转", row)
      window.open(this.BLOG_WEB_URL + "/#/info?blogUid=" + row.uid);
    },
    // 跳转到前端页面
    goPage: function (type, blog) {
      switch (type) {
        case 'MESSAGE_BOARD': {
          window.open(this.BLOG_WEB_URL + "/#/messageBoard")
        }
          break;
        case 'ABOUT': {
          window.open(this.BLOG_WEB_URL + "/#/about")
        }
          break;
        case 'BLOG_INFO': {
          window.open(this.BLOG_WEB_URL + "/#/info?blogUid=" + blog.uid);
        }
          break;
      }
    },
    /**
     * 字典查询
     */
    getDictList: function () {
      var dictTypeList = ['sys_comment_type', 'sys_comment_source', 'sys_params_status']
      getListByDictTypeList(dictTypeList).then(response => {
        if (response.code == this.$ECode.SUCCESS) {
          var dictMap = response.data;
          this.commentTypeDictList = dictMap.sys_comment_type.list
          this.commentSourceDictList = dictMap.sys_comment_source.list
          this.paramsStatusDictList = dictMap.sys_params_status.list;
          if (dictMap.sys_comment_type.defaultValue) {
            this.commentTypeDefaultValue = dictMap.sys_comment_type.defaultValue
            this.queryParams.type = this.commentTypeDefaultValue
            this.commentList()
          }
          if (dictMap.sys_params_status.defaultValue) {
            this.paramsStatusDefault = parseInt(dictMap.sys_params_status.defaultValue);
          }
        }
      });
    },
    commentList: function () {
      let params = {}
      params.keyword = this.queryParams.content
      if (this.queryParams.source == null || this.queryParams.source == undefined || this.queryParams.source == '') {
        params.source = "all"
      } else {
        params.source = this.queryParams.source
      }
      params.userName = this.queryParams.userName
      params.type = this.queryParams.type
      params.status = this.queryParams.status
      params.currentPage = this.currentPage
      params.pageSize = this.pageSize

      getCommentList(params).then(response => {
        if (response.code == this.$ECode.SUCCESS) {
          this.tableData = response.data.records;
          this.currentPage = response.data.current;
          this.pageSize = response.data.size;
          this.total = response.data.total;
        }
      });
    },
    subStr(str, index) {
      if (str == null || str == undefined) {
        return "";
      }
      if (str.length < index) {
        return str;
      } else {
        return str.substring(0, index) + ".."
      }
    },
    handleFind: function () {
      this.commentList();
    },
    handleReply: function (row) {
      this.$commonUtil.message.info("回复功能待完善")
      console.log("点击了回复");
    },
    handleDelete: function (row) {
      this.$confirm("此操作将把该评论删除, 是否继续?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          let params = {}
          params.uid = row.uid
          deleteComment(params).then(response => {
            this.$commonUtil.message.success(response.message)
            this.commentList();
          })
        })
        .catch(() => {
          this.$commonUtil.message.info("已取消删除")
        });
    },
    handleDeleteBatch: function () {
      var that = this;
      if (that.multipleSelection.length <= 0) {
        this.$message({
          type: "error",
          message: "请先选中需要删除的内容！"
        });
        return;
      }
      this.$confirm("此操作将把选中的评论删除, 是否继续?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          deleteBatchComment(that.multipleSelection).then(response => {
            this.$commonUtil.message.success(response.message)
            that.commentList();
          });
        })
        .catch(() => {
          this.$commonUtil.message.info("已取消删除")
        });
    },
    handleCurrentChange: function (val) {
      this.currentPage = val;
      this.commentList();
    },
    // 改变多选
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },
    isExist: function (blog) {
      console.log(blog)
      if (blog.status == 0) {
        return false;
      }
    }
  }
};
</script>
<style>
@import "../../assets/css/emoji.css";

.emoji-item-common {
  background: url("../../assets/img/emoji_sprite.png");
  display: inline-block;
}

.emoji-item-common:hover {
  cursor: pointer;
}

.emoji-size-small {
  zoom: 0.3;
  margin: 5px;
  vertical-align: middle;
}
</style>

