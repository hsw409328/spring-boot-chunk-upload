<template>
  <div class="hello">
    <uploader :options="options" @file-added="fileAdded" @file-success="fileSuccess" :auto-start="false" @ class="uploader-example">
      <uploader-unsupport></uploader-unsupport>
      <uploader-drop>
        <uploader-btn
          :single=true
          :attrs="attrs"
        >选择文件</uploader-btn>
        <span style="font-size: 9pt;color: #5c5c5c">请上传文件</span>
      </uploader-drop>
      <uploader-list :style="listHidden"></uploader-list>
    </uploader>
  </div>
</template>

<script>
import axios from 'axios'
import qs from 'qs'
export default {
  data () {
    return {
      listHidden: 'display: none',
      uploadId: 0,
      maxCounts: 0,
      options: {
        target: '//localhost:8080/file/big/chunk',
        testChunks: false,
        simultaneousUploads: 10,
        chunkSize: 1 * 1024 * 1024,
        headers: {'Authorization': 'Bearer f0346dad-f547-4791-b308-efc7f571caad'},
        query: {uploadId: 0}
      },
      attrs: {
        accept: 'image/*'
      },
      selectFileName: ''
    }
  },
  methods: {
    // 上传完成
    complete () {
      console.log('complete', arguments)
    },
    fileAdded (file) {
      if (this.maxCounts >= 1) {
        alert('最多可以上传1个文件')
        // 这里有BUG
        return false
      }
      var me = this
      me.maxCounts += 1
      me.selectFileName = file.name
      axios.post('//localhost:8080/file/big/init', qs.stringify({fileName: file.name}), {
        headers: {
          // 可以写自己的头
        }
      }).then(function (response) {
        me.uploadId = response.data
        me.options.query.uploadId = response.data
        me.listHidden = 'display: block'
      }).catch(function (error) {
        console.log(error)
      })
      return true
    },
    // 一个根文件（文件夹）成功上传完成。
    fileSuccess (rootFile, file, message, chunk) {
      var me = this
      console.log('file complete', arguments)
      axios.post('//localhost:8080/file/big/merge', qs.stringify({
        uploadId: me.uploadId
      }), {
        headers: {
          // 可以写自己的头
        }
      }).then(function (response) {
        console.log(response)
      }).catch(function (error) {
        console.log(error)
      })
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  h1, h2 {
    font-weight: normal;
  }

  .uploader-example {
    width: 880px;
    padding: 15px;
    margin: 40px auto 0;
    font-size: 12px;
  }
  /deep/ .uploader-drop {
    position: relative;
    padding: 10px;
    overflow: hidden;
    border: 1px solid ;
    background-color: #f1f1f1;
  }

  .uploader-example .uploader-btn {
    margin-right: 4px;
  }

  .uploader-example .uploader-list {
    max-height: 440px;
    overflow: auto;
    overflow-x: hidden;
    overflow-y: auto;
  }
  /*/deep/ .uploader-file-actions .uploader-file-remove {*/
  /*  display: none!important;*/
  /*}*/
  /*/deep/ .uploader-file-actions .uploader-file-pause {*/
  /*  display: none!important;*/
  /*}*/

  ul {
    list-style-type: none;
    padding: 0;
  }

  li {
    display: inline-block;
    margin: 0 10px;
  }

  a {
    color: #42b983;
  }
</style>
