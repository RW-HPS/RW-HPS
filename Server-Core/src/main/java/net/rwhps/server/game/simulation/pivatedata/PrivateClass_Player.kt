package net.rwhps.server.game.simulation.pivatedata

import com.corrodinggames.rts.game.n
import net.rwhps.server.game.simulation.gameFramework.GameEngine
import net.rwhps.server.util.WaitResultUtil
import net.rwhps.server.util.alone.annotations.GameSimulationLayer
import net.rwhps.server.util.log.exp.ImplementedException

internal class PrivateClass_Player(private val playerData: n) {
    @GameSimulationLayer.GameSimulationLayer_KeyWords("is victorious!")
    val survive get() = (!playerData.b() && !playerData.G && !playerData.F && !playerData.E)

    @GameSimulationLayer.GameSimulationLayer_KeyWords("Units Killed")
    private val gameStatistics = GameEngine.gameStatistics.a(playerData)
    /** 单位击杀数 */
    val unitsKilled get() = gameStatistics.c
    /** 建筑毁灭数 */
    val buildingsKilled get() = gameStatistics.d
    /** 单实验单位击杀数 */
    val experimentalsKilled get() = gameStatistics.e
    /** 单位被击杀数 */
    val unitsLost get() = gameStatistics.f
    /** 建筑被毁灭数 */
    val buildingsLost get() = gameStatistics.g
    /** 单实验单位被击杀数 */
    val experimentalsLost get() = gameStatistics.h

    /** 玩家的资金 */
    var credits
        get() = playerData.o.toInt()
        set(value) { playerData.o = value.toDouble() }

    companion object {
        @Throws(ImplementedException.PlayerImplementedException::class)
        internal fun getPlayerData(site: Int): PrivateClass_Player = WaitResultUtil.waitResult { PrivateClass_Player(n.k(site)) } ?: throw ImplementedException.PlayerImplementedException("[PlayerData-New] Player is invalid")
    }
}