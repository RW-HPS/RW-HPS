package net.rwhps.server.plugin.beta.httpapi.handlers.post

import net.rwhps.server.data.global.Data
import net.rwhps.server.func.StrCons
import net.rwhps.server.net.http.AcceptWeb
import net.rwhps.server.net.http.SendWeb
import net.rwhps.server.plugin.beta.httpapi.handlers.BasePostHandler
import net.rwhps.server.plugin.beta.httpapi.responses.BaseResp
import net.rwhps.server.util.game.CommandHandler
import net.rwhps.server.util.inline.toPrettyPrintingJson
import java.net.URLDecoder

class CommandHandler : BasePostHandler() {
    override fun post(accept: AcceptWeb, send: SendWeb) {
        super.post(accept, send)
        val param = stringResolveToJson(accept)
        val command = URLDecoder.decode(param.getString("exec"), "UTF-8")
        if (command.isEmpty()) send(send,BaseResp(data = "参数错误").toPrettyPrintingJson())
        var text = ""
        val response = Data.SERVER_COMMAND.handleMessage(command, StrCons { obj: String -> text += "$obj${Data.LINE_SEPARATOR}" })
        if (response != null && response.type != CommandHandler.ResponseType.noCommand) {
            if (response.type != CommandHandler.ResponseType.valid) {
                text = when (response.type) {
                    CommandHandler.ResponseType.manyArguments -> {
                        "Too many arguments. Usage: " + response.command.text + " " + response.command.paramText
                    }
                    CommandHandler.ResponseType.fewArguments -> {
                        "Too few arguments. Usage: " + response.command.text + " " + response.command.paramText
                    }
                    else -> {
                        "Unknown command. Check help"
                    }
                }
            }
        }
        if (text.isNotEmpty()) {
            send(send,BaseResp(data = text).toPrettyPrintingJson())
        }
    }
}