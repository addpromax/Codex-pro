/*
 * This file is part of Vulpecula, licensed under the MIT License.
 *
 *  Copyright (c) 2018 Bkm016
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.script.kether.action.game.vector

/**
 * Vulpecula
 * com.mcstarrysky.aiyatsbus.module.kether.action.vector
 *
 * @author Lanscarlos
 * @since 2023-03-22 15:30
 */
object ActionVectorDivide : ActionVector.Resolver {

    override val name: Array<String> = arrayOf("div", "divide")

    override fun resolve(reader: ActionVector.Reader): ActionVector.Handler<out Any?> {
        val source = reader.source().accept(reader)

        return reader.transfer {
            if (expectToken("with", "by")) {
                /*
                * 向量对象
                * vec div &vec with &target
                * vec div &vec with 0,0,0
                * */
                combine(
                    source,
                    vector(display = "vector target")
                ) { vector, target ->
                    vector.divide(target)
                }
            } else if (peekToken().matches("-?\\d+(.\\d+)?".toRegex())) {
                /*
                * 数字
                * vec div &vec x y z
                * */
                combine(
                    source,
                    double(1.0), // x
                    double(1.0), // y
                    double(1.0)  // z
                ) { vector, x, y, z ->
                    vector.x *= x
                    vector.y *= y
                    vector.z *= z
                    vector
                }
            } else {
                /*
                * 参数
                * vec div &vec -x &x -y &y -z &z
                * */
                combine(
                    source,
                    argument("x", then = double(), def = 1.0),
                    argument("y", then = double(), def = 1.0),
                    argument("z", then = double(), def = 1.0),
                ) { vector, x, y, z ->
                    vector.x *= x
                    vector.y *= y
                    vector.z *= z
                    vector
                }
            }
        }
    }
}