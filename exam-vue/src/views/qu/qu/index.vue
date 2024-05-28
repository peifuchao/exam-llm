<template>
  <div>

    <data-table ref="pagingTable" :options="options" :list-query="listQuery" @multi-actions="handleMultiAction">
      <template #filter-content>

        <el-row>
          <el-col :span="24">

            <el-select v-model="listQuery.params.quType" class="filter-item" clearable>
              <el-option v-for="item in quTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>

            <repo-select v-model="listQuery.params.repoIds" :multi="true" />

            <el-input v-model="listQuery.params.content" placeholder="题目内容" style="width: 200px;" class="filter-item" />

            <el-button-group class="filter-item" style="float:  right">
              <el-button size="mini" icon="el-icon-upload2" @click="showImport">导入</el-button>
              <el-button size="mini" icon="el-icon-download" @click="exportExcel">导出</el-button>
            </el-button-group>

          </el-col>
        </el-row>


      </template>

      <template #data-columns>

        <el-table-column label="题目类型" align="center" width="100px">
          <template v-slot="scope">
            {{ scope.row.quType | quTypeFilter() }}
          </template>
        </el-table-column>

        <el-table-column label="题目内容" show-overflow-tooltip>
          <template v-slot="scope">
            <router-link :to="{ name: 'UpdateQu', params: { id: scope.row.id } }">
              {{ scope.row.content }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" align="center" prop="createTime" width="180px" />

      </template>

    </data-table>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="30%">

      <el-form label-position="left" label-width="100px">

        <el-form-item label="操作题库" prop="repoIds">
          <repo-select v-model="dialogRepos" :multi="true" />
        </el-form-item>

        <el-row>
          <el-button type="primary" @click="handlerRepoAction">保存</el-button>
        </el-row>

      </el-form>

    </el-dialog>

    <el-dialog :visible.sync="importVisible" title="导入试题" width="30%">

      <el-row>
        <el-button type="primary" @click="chooseFile">上传导入</el-button>
        <el-button type="warning" @click="downloadTemplate">下载导入模板</el-button>
        <input ref="upFile" class="file" name="file" type="file" style="display: none" @change="doImport">
      </el-row>

    </el-dialog>

    <el-dialog destroy-on-close :visible.sync="options.showAddQu" title="题目约束" style="user-select: text;" width="400px"
      center>
      <el-form :inline="true" :model="quCon" status-icon label-width="120px">
        <el-form-item label="题目类型">
          <el-select v-model="quCon.type" placeholder="请选择题目类型" style="width: 189px;">
            <el-option v-for="t in quTypes" :key="t.value" :label="t.label" :value="t.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-input v-model="quCon.subject" placeholder="请输入科目"></el-input>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="quCon.level" placeholder="请选择题目难度" style="width: 189px;">
            <el-option v-for="l in leves" :key="l.value" :label="l.label" :value="l.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-select v-model="quCon.number" placeholder="请选择题目数量" style="width: 189px;">
            <el-option v-for="i in nums" :key="i" :label="i.toString()" :value="i">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="归属题库" prop="repoIds">

          <repo-select v-model="postForm.repoIds" :multi="true" />

        </el-form-item>
      </el-form>
      <template>
        <span class="dialog-footer">

          <el-button @click="closeAddQu">取消</el-button>

          <el-button type="primary" @click="sureHandler(quCon)" style="margin-left: 60%;">确定</el-button>

        </span>
      </template>
    </el-dialog>

    <el-dialog :visible.sync="loadingDialogVisible" :modal="false" :close-on-click-modal="false"
      :close-on-press-escape="false" title="请稍等" width="400px" :show-close="false" style="border-radius: 3px;" center>
      <span>正在生成，请稍后...</span>
    </el-dialog>



  </div>
</template>




<script>
import DataTable from '@/components/DataTable'
import RepoSelect from '@/components/RepoSelect'
import { batchAction } from '@/api/qu/repo'
import { exportExcel, importExcel, importTemplate, generate, fetchDetail } from '@/api/qu/qu'

export default {
  name: 'QuList',
  components: { RepoSelect, DataTable },
  data() {
    return {

      dialogTitle: '加入题库',
      dialogVisible: false,
      importVisible: false,
      loadingDialogVisible: false,
      dialogRepos: [],
      dialogQuIds: [],
      dialogFlag: false,
      quCon: {
        type: '',
        subject: '',
        level: '',
        number: '',
        repos: []
      },
      postForm: {
        repoIds: [],
        tagList: [],
        answerList: []
      },
      rules: {
        repoIds: [
          { required: true, message: '至少要选择一个题库！' }
        ]
      },

      listQuery: {
        current: 1,
        size: 10,
        params: {
          content: '',
          quType: '',
          repoIds: []
        }
      },

      quTypes: [
        {
          value: 1,
          label: '单选题'
        },
        {
          value: 2,
          label: '多选题'
        },
        {
          value: 3,
          label: '判断题'
        }
      ],
      leves: [
        {
          value: 1,
          label: '普通'
        },
        {
          value: 2,
          label: '较难'
        }
      ],
      nums: Array.from({ length: 10 }, (_, i) => i + 1),

      options: {

        // 可批量操作
        multi: true,

        // 批量操作列表
        multiActions: [
          {
            value: 'add-repo',
            label: '加入题库..'
          },
          {
            value: 'remove-repo',
            label: '从..题库移除'
          },
          {
            value: 'delete',
            label: '删除'
          }
        ],
        // 列表请求URL
        listUrl: '/exam/api/qu/qu/paging',
        // 删除请求URL
        deleteUrl: '/exam/api/qu/qu/delete',
        // 添加数据路由
        addRoute: 'AddQu',
        isAdd: true,
        showAddQu: false
      }
    }
  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== 'undefined') {
      this.quTypeDisabled = true
      this.fetchData(id)
    }
  },
  methods: {

    handleMultiAction(obj) {
      if (obj.opt === 'add-repo') {
        this.dialogTitle = '加入题库'
        this.dialogFlag = false
      }

      if (obj.opt === 'remove-repo') {
        this.dialogTitle = '从题库移除'
        this.dialogFlag = true
      }

      this.dialogVisible = true
      this.dialogQuIds = obj.ids
    },

    handlerRepoAction() {
      const postForm = { repoIds: this.dialogRepos, quIds: this.dialogQuIds, remove: this.dialogFlag }

      batchAction(postForm).then(() => {
        this.$notify({
          title: '成功',
          message: '批量操作成功！',
          type: 'success',
          duration: 2000
        })

        this.dialogVisible = false
        this.$refs.pagingTable.getList()
      })
    },

    exportExcel() {
      // 导出当前查询的数据
      exportExcel(this.listQuery.params)
    },

    downloadTemplate() {
      importTemplate()
    },

    showImport() {
      this.importVisible = true
    },

    // 只是为了美化一下导入按钮
    chooseFile: function () {
      this.$refs.upFile.dispatchEvent(new MouseEvent('click'))
    },

    doImport(e) {
      const file = e.target.files[0]

      importExcel(file).then(res => {
        if (res.code !== 0) {
          this.$alert(res.data.msg, '导入信息', {
            dangerouslyUseHTMLString: true
          })
        } else {
          this.$message({
            message: '数据导入成功！',
            type: 'success'
          })

          this.importVisible = false
          this.$refs.pagingTable.getList()
        }
      })
    },
    sureHandler(data) {
      this.quCon.repos = this.postForm.repoIds
     data.repos = this.postForm.repoIds

      if (this.quCon.type === '') {
        this.showMessage("请选择题目类型")
        return
      }
      if (this.quCon.subject === '') {
        this.showMessage("请输入科目")
        return
      }
      if (this.quCon.level === '') {
        this.showMessage("请选择题目难度")
        return
      }
      if (this.quCon.number === '') {
        this.showMessage("请选择题目数量")
        return
      }
      if (this.quCon.repos.length === 0) {
        this.showMessage("请选择归属题库")
        return
      }
      this.loadingDialogVisible = true
      generate(data).then(() => {
        this.$notify({
          title: '成功',
          message: '生成成功！',
          type: 'success',
          duration: 2000
        })
        this.options.showAddQu = false
        this.$refs.pagingTable.getList()
        this.loadingDialogVisible = false
      })
    },
    closeAddQu() {
      this.options.showAddQu = false
    },
    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data
      })
    },
    showMessage(message) {
      this.$notify({
        title: '提示',
        message: message,
        type: 'failure',
        duration: 2000
      })
    }
  }
}
</script>
