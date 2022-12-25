[Chinese version / 中文介绍](README-CN.md)  
**Welcome to provide better translation**

# New version under development
- [x] Use Headless Lwjgl
  
Welcome everyone to come to PR. 
We are currently in need of refactoring (EventNetDocs), welcome to provide better ideas

![](https://img.shields.io/github/stars/RW-HPS/RW-HPS.svg)
![](https://github.com/RW-HPS/RW-HPS/actions/workflows/gradle.yml/badge.svg?branch=master)
![](https://jitpack.io/v/RW-HPS/RW-HPS.svg)
![](https://app.fossa.com/api/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS.svg?type=shield)

# RW-HPS Project
RW-HPS is a Rusted Warfare game server, used to quickly set up high-performance game servers on servers running Java8

## Features
### High performance
Ditch the <Java -Blocking IO / BIO> development approach used by the game itself and use <Java -Not-Blocking IO / NIO> to increase throughput and reduce latency
### Plugin framework
Despite most of the things you can do in a normal game, you may also do a lot of other things on this server because there's a plugin framework which allows you to extend the functionality of this server. Furthermore, you can even implement your own actions!

## Actions List

<details>
  <summary>Actions List</summary>  

**Message**
- Team Chat
- All Chat
- Map location

**Game**
- Basic Game
- Game reconnection
- Check Data
- Custom map
- Load Save Game
- Mods Support
- Replay generate

**Ex**
- Loading plugins
- Player jump server
- Map generation unit
- RELAY Server

**Other**
- BanUUID
- BanIP
- Mute Player

</details>

#### Currently to be completed
- [ ] Vote (Bug :( )
- [ ] Hot modify game progress
- [ ] GamePanel

### Actions that won't be supported
- Anything related to real money

## Needs Help?
- Documentation: [docs](docs/en/README.md)  
- Update log: [release](https://github.com/RW-HPS/RW-HPS/releases)
- Development milestones: [milestones](https://github.com/RW-HPS/RW-HPS/milestones)
- Discussion:
  > The developemenet team is actively answering questions on Github Discussions, please feel free to share you ideas about this project.  
  > Email contact: dr@der.kim      
  > Tencent QQ Group: [901913920](https://qm.qq.com/cgi-bin/qm/qr?k=qhJ6ekYF9pD9jO6j8H2rZw8ePAVypoU0&jump_from=webapi)  
  > <del>Telegram Group: [RW-HPS](https://t.me/RW_HPS) </del>  
  > Discord: [RW-HPS](https://discord.gg/VwwxJhVG64)  
- Mirrors:
  [Github](https://github.com/RW-HPS/RW-HPS) ; [Gitee](https://gitee.com/derdct/RW-HPS)
  
### Used by whom
- Tiexiu.xyz
    - [简幻欢](https://sfe.simpfun.cn)  
- RELAY-Unofficial
    - **RelayCN-Unofficial IP** - [SimpFun Cloud](https://cloud.simpfun.cn) : relay.der.kim
    - **RelayRU-Unofficial IP** - `kaif.cloud`(Not URL)

## Recommended Configuration

| Configuration 		| CPU                     | RAM 	| System 			 | Disk 	  | Java Version |
|:--- 		|:------------------------|:---     |:-----------|:--------|:---       |
| Recommended 	| ARMv7 Processor rev 5 + | 512MB      | Any        | 256M HDD | Java 11   |
| Minimum 	| Any                     | 256M      | Any        | 128M HDD | Java 11   |

## Server Command List
Depending on the startup protocol, the server command will change accordingly    
Please use help in the console to see more  

## Game Command List
<details>
  <summary>Game Command List</summary>  

| Command 			| Parameter 												 | Information 										 |
|:---           |:--- 												 |:--- 										 |
| help      |   | Get help 									 |
There are many commands not shown here. I suggest you test them yourself  
Please use .help in the game to see more  
</details>

## version collection
The game corresponds to the final server version collection

| Game Version |   RW-HPS version   |                              Download Link                            |
|:------------:|:------------------:|:---------------------------------------------------------------------:|
|   ~~1.14~~   | ~~6.0.0-M3（beta）~~ |                                 Delete                                |
| ~~1.15.P8~~  |     ~~6.0.1~~      |                                 Delete                                |
|     1.15     |      1.0.0      | [Go to download](https://github.com/RW-HPS/RW-HPS/releases/tag/1.0.0) |

## Sponsorship
RW-HPS is a open source project released under AGPL V3. However, the amount of work required to maintain and develop new features for the project is not sustainable without your generous dedications.
Please note that donations are completely voluntary, thus sponsors do not have additional privileges and all the features are available without sponsorship.

We receive donations through the following channels：
+ [Ko-Fi](https://ko-fi.com/derdct)
+ [CN - 爱发电](https://afdian.net/@derdct)

## Contribution
All kinds of contributions are welcomed.
If you hold an interest in helping us implementing RW-HPS on JS, iOS or Native platforms, please email us .
If you meet any problem or have any questions, feel free to file an issue. Our goal is to make RW-HPS easy to use `RW-HPS@der.kim`

## Special Thanks
> [IntelliJ IDEA](https://zh.wikipedia.org/zh-hans/IntelliJ_IDEA) is an IDE that maximizes the productivity of developers in all aspects, and is suitable for JVM platform languages。

[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=rw-hps)

## Statements
### For educational purposes only
- RW-HPS is a free and open source project and is only for educational purposes. You shall not use this project for any illegal purposes. The author or the developement team of this project will not be held responsible for any damages.


## Stargazers over time

[![Stargazers over time](https://starchart.cc/RW-HPS/RW-HPS.svg)](https://starchart.cc/RW-HPS/RW-HPS)

## License
- [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)
<details>
  <summary>AGPL-3.0</summary>

```
Copyright (C) 2020-2022 RW-HPS Team and contributors.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
</details>


- [EULA](https://github.com/RW-HPS/RW-HPS/blob/master/Server/src/main/resources/eula/China.md)
<details>
  <summary>EULA</summary>

```
版权所有©2022 RW-HPS.Team <RW-HPS@der.kim>

允许在其遵守CC BY-NC-SA 4.0协议的同时，每个人复制和分发此许可证文档的逐字记录副本，且允许对其进行更改，但必须保留其版权信息与原作者。

请务必仔细阅读和理解通用用户协议书中规定的所有权利和限制。在使用前，您需要仔细阅读并决定接受或不接受本协议的条款。除非或直至您接受本协议的条款，否则本软件及其相关副本、相关程序代码或相关资源不得在您的任何终端上下载、安装或使用。

您一旦下载、使用本软件及其相关副本、相关程序代码或相关资源，即表示您同意接受本协议各项条款的约束。如您不同意本协议中的条款，您则应当立即删除本软件、附属资源及其相关源代码。

本软件权利只许可使用，而不出售。

本协议与GNU Affero通用公共许可证(即AGPL协议)共同作为本软件与您的协议，且本协议与AGPL协议的冲突部分均按照本协议约束。您必须同时同意并遵守本协议与AGPL协议，否则，您应立即卸载、删除本软件、附属资源及其相关源代码。
```
</details>

<details>
  <summary>FOSSA Status</summary>

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS?ref=badge_large)
</details>
