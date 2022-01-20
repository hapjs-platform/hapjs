#!/usr/bin/env python
#
# Copyright (c) 2021, the hapjs-platform Project Contributors
# SPDX-License-Identifier: Apache-2.0
#

import os
import sys
import re
import linecache

V8_REL_PATH = 'external/v8'
MK_DIRS = []
LOCAL_PROPERTIES_DIRS = []
CONFIGS = {}

DIR_KEYS = [
  'root-path',
  'v8-tools-path'
]

def parse_args(argv):
  global CONFIGS
  count = len(argv)
  if (count < len(DIR_KEYS) * 2):
    print('need parameters --root-path, --v8-tools-path')
    sys.exit(1)
  i = 0
  while i < count:
    arg = argv[i]
    if arg[:2] == '--':
      key = arg[2:]
      if (key in DIR_KEYS):
        CONFIGS[key] = argv[i + 1]
        i = i + 2
      else:
        print('arg error', arg)
        sys.exit(1)


def check_v8_dir():
  v8_tools_path = CONFIGS['v8-tools-path']
  if os.path.exists(v8_tools_path):
    print('v8 directory already exists')
    return
  else:
    download_v8()


def download_v8():
  username = get_user_name()
  print('username is :', username)
  git_ssh_url = 'ssh://' + username + "@gerrit.sodajs.org:29418/hap/external/v8"
  cmds = ['git', 'clone', git_ssh_url]
  command_line = ' '.join(cmds)
  print(command_line)
  v8_dir_pos = CONFIGS['v8-tools-path'].rfind('v8')
  v8_dir = CONFIGS['v8-tools-path'][:v8_dir_pos]
  os.chdir(v8_dir)
  os.system(command_line)


def get_user_name():
  cmds = ['git', 'config', 'user.name']
  command_line = ' '.join(cmds)
  print(command_line)
  username = os.popen(command_line).read()
  if (username == ''):
    print('get user name error')
    sys.exit(1)
  username = username.strip('\n')
  return username


def write_local_properties(local_properties_dir):
  SPECIAL_TAG = '# add by ndk_build.py'
  has_special_tag = False
  ndk_dir_pattern = re.compile('[ ]*ndk.dir[ ]*=')
  lp_path = os.path.join(local_properties_dir, 'local.properties')
  comment_line = SPECIAL_TAG + '\n'
  ndk_line = 'ndk.dir='+os.path.join(CONFIGS['v8-tools-path'], 'ndk') + '\n'

  file_content = linecache.getlines(lp_path)
  i = 0
  while i < len(file_content):
    if (file_content[i].find(SPECIAL_TAG) >= 0 and i < len(file_content) - 1):
      file_content[i + 1] = ndk_line
      i = i + 2
      has_special_tag = True
      continue
    if (re.match(ndk_dir_pattern, file_content[i])):
      file_content[i] = '#' + file_content[i]
      i = i + 1
      continue
    i = i + 1

  with open(lp_path, 'w') as f:
    if len(file_content) > 0:
      f.writelines(file_content)
    if (has_special_tag == False):
      f.write(comment_line)
      f.write(ndk_line)
  return


def add_local_properties():
  for lp_dir in LOCAL_PROPERTIES_DIRS:
    write_local_properties(lp_dir)
    print('add local.properties in', lp_dir)


def filter_local_properties(lp_dir):
  # remove repeat dir
  for lp in LOCAL_PROPERTIES_DIRS:
    if (lp_dir != lp and lp_dir.find(lp) >=0):
      return False

  return True


def get_local_properties_dirs_and_mk_dirs():
  global MK_DIRS, LOCAL_PROPERTIES_DIRS
  for relpath, dirs, files in os.walk(CONFIGS['root-path']):
    if ('build.gradle' in files and relpath.find(V8_REL_PATH) < 0):
      LOCAL_PROPERTIES_DIRS.append(os.path.join(CONFIGS['root-path'],\
          relpath));
    if ('Application.mk' in files and relpath.find(V8_REL_PATH) < 0):
      MK_DIRS.append(os.path.join(CONFIGS['root-path'], relpath))
  # remove unnecessary build.gradle dir
  LOCAL_PROPERTIES_DIRS = \
      filter(filter_local_properties, LOCAL_PROPERTIES_DIRS)


def check_application_mk():
  SPECIAL_TAG = '#SPECIAL'
  special_app_stl_pattern = re.compile('APP_STL[ ]*' + SPECIAL_TAG)
  default_app_stl_pattern = re.compile('APP_STL[ ]*[:]?=[ ]*c\+\+_shared')
  for mk_dir in  MK_DIRS:
    mk_path = os.path.join(mk_dir, 'Application.mk')
    with open(mk_path, 'r') as f:
      for line in f:
        line = line.strip()
        if (line.startswith('#')):
          continue
        if (re.match(special_app_stl_pattern, line)):
          continue
        if (line.startswith('APP_STL')):
          if (re.match(default_app_stl_pattern, line) < 0):
            print('APP_STL is not c++_shared in',line,  mk_path)
            sys.exit(1)
  print('Applicatioin.mk is correct')
  return


def main():
  parse_args(sys.argv[1:])
  get_local_properties_dirs_and_mk_dirs()
  check_v8_dir()
  check_application_mk()
  add_local_properties()


if __name__ == '__main__':
  main()

