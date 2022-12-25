/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.core;

import kotlin.Unit;
import net.rwhps.server.core.thread.CallTimeTask;
import net.rwhps.server.core.thread.Threads;
import net.rwhps.server.core.thread.TimeTaskData;
import net.rwhps.server.data.global.Data;
import net.rwhps.server.data.global.NetStaticData;
import net.rwhps.server.func.StrCons;
import net.rwhps.server.game.simulation.gameFramework.GameData;
import net.rwhps.server.net.core.IRwHps;
import net.rwhps.server.util.log.Log;

/**
 * @author RW-HPS/Dr
 */
public class NetServer {

    static String userId;

    public static void closeServer() {
        if (Data.game != null) {
            Data.INSTANCE.setExitFlag(true);

            TimeTaskData.INSTANCE.stopCallTickTask();

            Call.killAllPlayer("Server Close");

            NetStaticData.startNet.eachAll(e ->{
                    e.stop();
                    return Unit.INSTANCE;
            });
            NetStaticData.startNet.clear();
            NetStaticData.INSTANCE.setServerNetType(IRwHps.NetType.NullProtocol);
            Threads.closeNet();
            //Threads.newThreadCoreNet();

            GameData.clean();

            Threads.closeTimeTask(CallTimeTask.AutoCheckTask);
            Threads.closeTimeTask(CallTimeTask.CallPingTask);
            Threads.closeTimeTask(CallTimeTask.CallTeamTask);
            Threads.closeTimeTask(CallTimeTask.PlayerAfkTask);
            Threads.closeTimeTask(CallTimeTask.GameOverTask);
            Threads.closeTimeTask(CallTimeTask.AutoStartTask);
            Threads.closeTimeTask(CallTimeTask.AutoUpdateMapsTask);

            Data.SERVER_COMMAND.handleMessage("uplist remove", (StrCons) Log::clog);

            Data.game.getPlayerManage().playerGroup.clear();
            Data.game.getPlayerManage().playerAll.clear();
            Data.game = null;

            System.gc();
            Log.clog("Server closed");
        }
    }

    public static void reLoadServer() {
        if (Data.vote!= null) {
            Data.vote.stopVote();
        }
        TimeTaskData.INSTANCE.stopCallTickTask();
        Threads.closeTimeTask(CallTimeTask.GameOverTask);
        Threads.closeTimeTask(CallTimeTask.AutoCheckTask);


        Call.killAllPlayer();
        Data.game.re();
        Data.game.setStartGame(false);

        synchronized (net.udp.Data.waitData) {
            net.udp.Data.waitData.notify();
        }

        Log.saveLog();

        Log.clog("[Server Gameover completed]");
    }
}
