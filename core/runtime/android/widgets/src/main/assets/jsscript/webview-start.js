/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
// native调用system.onmessage时，如果开发者没有绑定system.onmessage，暂时缓存，开发者绑定后发出
(function(){
  let _onmessage = system.onmessage
  const pendingMsgList = []
  const defaultOnmessage = function(data){
    pendingMsgList.push(data)
  }
  function processPendingMsg(){
    while(pendingMsgList.length > 0){
      const data = pendingMsgList.shift()
      _onmessage(data)
    }
  }

  Object.defineProperty(system, 'onmessage', {
    set(v){
      _onmessage = v
      if(pendingMsgList.length > 0 && typeof _onmessage === 'function'){
        setTimeout(function(){
          processPendingMsg()
        }, 10)
      }
    },
    get(){
      if(typeof _onmessage === 'function'){
        return _onmessage
      } else{
        return defaultOnmessage
      }
    }
  })
})()