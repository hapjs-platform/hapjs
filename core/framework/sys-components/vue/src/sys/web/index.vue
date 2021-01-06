<template>
  <div class="doc-page" style="width:100%;flex-direction: column;">
    <web
      class="web-page"
      style="width: 100%; height: 0; flex: 1;"
      :src="url"
      :allowthirdpartycookies="allowthirdpartycookies"
      :showloadingdialog="showloadingdialog"
      :useragent="useragent"
      @titlereceive="onTitleReceived"
    ></web>
  </div>
</template>

<script>
  export default {
    data() {
      return {
        url: '',
        allowthirdpartycookies: '',
        titleBarText: '',
        showloadingdialog: true,
        titleBarTextColor: '',
        titleBarBackgroundColor: '',
        useragent: ''
      }
    },
    mounted() {
      this.$page.setTitleBar({
        text: this.titleBarText,
        textColor: this.titleBarTextColor,
        backgroundColor: this.titleBarBackgroundColor,
      })
    },
    methods: {
      onTitleReceived(e) {
        const title = e.title || ''
        this.$page.setTitleBar({ text: title })
      }
    }
  }
</script>
