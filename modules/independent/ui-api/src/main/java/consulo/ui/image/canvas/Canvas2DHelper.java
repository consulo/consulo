/*
 * Copyright 2013-2018 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.image.canvas;

/**
 * @author VISTALL
 * @since 2018-06-15
 */
class Canvas2DHelper {
  private static class Radius {
    int tl;
    int tr;
    int br;
    int bl;

    private Radius(int tl, int tr, int br, int bl) {
      this.tl = tl;
      this.tr = tr;
      this.br = br;
      this.bl = bl;
    }
  }

  // from https://stackoverflow.com/a/3368118
  public static void roundRectangle(Canvas2D ctx, int x, int y, int width, int height, int rad) {
    Radius radius = new Radius(rad, rad, rad, rad);
    ctx.beginPath();
    ctx.moveTo(x + radius.tl, y);
    ctx.lineTo(x + width - radius.tr, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius.tr);
    ctx.lineTo(x + width, y + height - radius.br);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius.br, y + height);
    ctx.lineTo(x + radius.bl, y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius.bl);
    ctx.lineTo(x, y + radius.tl);
    ctx.quadraticCurveTo(x, y, x + radius.tl, y);
    ctx.closePath();
  }
}
