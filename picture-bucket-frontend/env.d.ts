/// <reference types="vite/client" />

// vue-cropper 没有自带类型声明，这里兜底声明为任意插件
declare module 'vue-cropper' {
  import type { Plugin } from 'vue'
  const VueCropper: Plugin
  export default VueCropper
}

// vue-cropper 包内部 index.ts 引用了同目录的 .vue 文件，补充声明避免 TS7016
declare module '*/vue-cropper.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<any, any, any>
  export default component
}
