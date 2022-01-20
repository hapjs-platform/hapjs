/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import Runtime from 'src/infras/runtime/index'
import Base64 from 'src/infras/bundles/canvas/base64'

import animation from 'src/infras/bundles/animation/index'
import canvas from 'src/infras/bundles/canvas/index'
import parser from 'src/infras/bundles/parser/index'

import { Node, Event, TouchEvent, DomDocument, freeze } from 'src/infras/runtime/dom/index'

import Listener from 'src/infras/runtime/listener'
import Streamer from 'src/infras/runtime/streamer'

import * as helper from 'src/infras/runtime/helper'
import * as misc from 'src/infras/runtime/dom/misc'

export { Runtime, Base64 }

export const runtime = {
  Listener,
  Streamer,
  helper,
  misc
}

export const dom = {
  Node,
  Event,
  TouchEvent,
  DomDocument,
  freeze
}

export const bundles = {
  animation,
  canvas,
  parser
}
