<template>
  <div>
    <div class="pagebg ab"></div>
    <div class="container">
      <h1 class="t_nav">
        <span>偶尔对自己好些，偷个小懒，抽点小疯，花点小钱，不算伤天害理。</span>
        <a href="/" class="n1">网站首页</a>
        <a href="javascript:void(0);" class="n2">放松一下</a>
      </h1>

      <div class="share">
        <ul id="licount">
          <li>
            <div class="shareli">
              <a href="https://server.wyong.top/Music/index.html" target="_blank">
                <i><img class="resImg" src="https://server.wyong.top/Music/icon.png"></i>
                <h2><b>点击页面或鼠标键盘，跟着节奏摇摆吧！！！</b></h2>
                <span>Music</span>
              </a>
            </div>
          </li>
          <li>
            <div class="shareli">
              <a href="https://server.wyong.top/2048/index.html" target="_blank">
                <i><img class="resImg" src="https://server.wyong.top/2048/icon.png"></i>
                <h2><b>开始游戏吧！！！</b></h2>
                <span>2048</span>
              </a>
            </div>
          </li>
        </ul>
      </div>

      <!--分页-->
      <div class="block" style="text-align: center;">
        <el-pagination
          @current-change="handleCurrentChange"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </div>

  </div>
</template>

<script>

export default {
  name: "share",
  data() {
    return {
      dataList: [],
      currentPage: 1,
      pageSize: 12,
      total: 0, //总数量
    };
  },
  components: {
  },
  created() {
  },
  mounted() {
    this.total=document.getElementById("licount").getElementsByTagName("li").length
  },
  methods: {
    //改变页码
    handleCurrentChange(val) {
      this.currentPage = val; //改变当前所指向的页数
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.share {
  min-height: 550px;
}

.resImg {
  width: 100%;
  height: 160px;
  vertical-align: middle;
}

.el-drawer {
  min-width: 400px;
}
</style>
