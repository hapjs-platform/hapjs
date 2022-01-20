#!/usr/bin/env bash

echo "编译: framework"
npm install
npm run h5:ol
npm run test

[[ "$?" -ne 0 ]] && {
  echo -e "framework: 运行npm任务失败";
  exit 1
}
exit 0;