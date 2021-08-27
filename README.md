## Language(语言)  

[跳转中文介绍](README-CN.md)  

# RW-HPS
![Github stars](https://img.shields.io/github/stars/RW-HPS/RW-HPS.svg)[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS?ref=badge_shield)

![](https://github.com/RW-HPS/RW-HPS/actions/workflows/gradle.yml/badge.svg?branch=master)
[![](https://jitpack.io/v/RW-HPS/RW-HPS.svg)](https://jitpack.io/#RW-HPS/RW-HPS)  

### **It is currently in KT rewriting status. Welcome to rewrite and submit pr**

## what is it?
This is a server based on the Rusted Warfare . You can run it on any device that can run Java 11
### What can it do
You can do a lot of interesting things on this server  
It can do most of the things you can do in a normal game  
You can do what you can't do in the original game  
You can also write plugin to expand the function of the server  
You can also customize your protocol implementation

## Statement
### All development is for learning, please do not use it for illegal purposes
- RW-HPS Is completely free and open source software, only for learning and entertainment purposes
- RW-HPS Will not be forced to charge fees in any way, or impose material conditions on users

## Present
Third party rust war server  
This is a netty based server  
It aims to provide better game experience for players as a high performance and high availability server

### Licenses
Used in this project  
GNU Affero General Public License v3.0
```
Copyright (C) 2020-2021 RW-HPS Team and contributors.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```


[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FRW-HPS%2FRW-HPS?ref=badge_large)

#### Game protocols that will not be supported
- Server List -> ADD List, Update List, Remove List  
- Money related, such as value-added services  

**All development is for learning, do not use for illegal purposes** 

`RW-HPS` is open sourced under the `AGPLv3` agreement. For the healthy development of the entire community，We **highly recommended**you do the following：

- **Indirect contact (including but not limited to using `Http API` or cross-process technology and bytecode modification) to `RW-HPS` software using `AGPLv3` open source**
- **For personal use, please join the server to use from RW-HPS**
- **Do not encourage, do not support all commercial use*

### Derivative software needs to declare and quote

- If the software package released by RW-HPS is quoted without modifying RW-HPS, the derivative project shall mention the use of RW-HPS in any part of the description。
- If the RW-HPS source code is modified and then released，**Or refer to the internal implementation of RW-HPS to release another project**，Then the derivative project must be**article head**or 'RW-HPS' related information**first Appearance**s position**clearly stated**from This Warehouse (`https://github.com/RW-HPS/RW-HPS`) Don’t distort or hide the fact that it’s free and open source。

## Protocol support

<details>
  <summary>Protocol support List</summary>  

**Message**
- Team Chat
- All Chat
- Map location
- Forbidden words

**Game**
- Unit movement
- Game reconnection
- Custom map

**Ex**
- Loading plugins
- Player jump server(TODO)
- Map generation unit(TODO)

**Other**
- BanUUID
- BanIP

</details>

## Start
- Development documentation: [docs](docs/zh/README.md)  
- Update log: [release](https://github.com/RW-HPS/RW-HPS/releases)  
- Development plan: [milestones](https://github.com/RW-HPS/RW-HPS/milestones)  
- Discuss:
  > Questions raised in GitHub discussions will be answered, and you are welcome to share your new ideas based on the project.  
  > Email contact: dr@der.kim  
  > Tencent QQ Group: [901913920](https://qm.qq.com/cgi-bin/qm/qr?k=qhJ6ekYF9pD9jO6j8H2rZw8ePAVypoU0&jump_from=webapi) (AGPLv3)  
  > <del>Telegram Group: [RW-HPS](https://t.me/RW_HPS) </del>  
  > Discord: [RW-HPS](https://discord.gg/VwwxJhVG64)
  >> Tencent QQ: A modern messaging software used by all Chinese netizens.  
- Mirroring:
  > [Github](https://github.com/RW-HPS/RW-HPS)  
  > [Gitee](https://gitee.com/derdct/RW-HPS)

## Run

| Configure 		| CPU             | RAM 	| SYSTEM 			| Disk 	| Java      |
|:--- 		|:---             |:---     |:---           |:---       |:---       |
| Recommended Configuration 	| ARMv7 Processor rev 5 +  | 128MB      | Linux~  | 64M HDD  | Java 11   |
| Minimum Configuration 	| ARMv7 Processor rev 5  | 64M      | Linux~  | 64M HDD  | Java 11   |

## Server Command List
<details>
  <summary>Server Command List</summary>  

| Command 					 | Parameter 																						 | Information 									 |
|:--- 					 |:--- 																						 |:--- 									 |
| help 		              |                                                  										 | Get help 		 |
| start                  |                                                  										 | Turn on the server 						 |
| say 		      | &lt;TEXT&gt;                                                  										 | Send messages in the name of Server 				 |
| giveadmin                | &lt;PlayerSerialNumber&gt; 																 | Transfer Admin       		         |
| restart 			 | 																							 | Restart server 				 |
| gameover 				 |  	 | Restart The Game               				 |
| clearbanip          		 |                                                  										 | Clean up the banned IP               	 |
| admin          		 |&lt;add/remove&gt; &lt;PlayerSite&gt;                                                  										 | Set up admin               			 |
| clearbanuuid          		 |                               	   											 | Clear banned uuid               			 |
| clearbanall          		 |                               	   											 | Empty ban               			 |
| ban          		 | &lt;PlayerSerialNumber&gt;                                 	   											 | Ban someone               			 |
| mute          		 |  &lt;PlayerSerialNumber&gt;  &lt;Time/s&gt;                             	   											 | Forbid sb from speaking               			 |
| kick          		 |  &lt;PlayerSerialNumber&gt;  &lt;Time/s&gt;                             	   											 | Kick               			 |
| isafk          		 |  &lt;off/on&gt;                             	   											 | Whether to enable AFK               			 |
| plugins          		 |                               	   											 | View the list of plugins               			 |
| players          		 |                               	   											 | View player list               			 |
| kill          		 | &lt;PlayerSerialNumber&gt;                             	   											 | Kill the player               			 |
| clearmuteall          		 |                               	   											 | Unmute all               			 |
| maps          		 |                               	   											 | View Custom Map               			 |
| reloadmaps          		 |                               	   											 | Reload map               			 |
| stop          		 |                               	   											 | Stop the server               			 |
Sorry, maybe more commands have not been added because the document has no time to update
</details>


## Game Command List
<details>
  <summary>Game Command List</summary>  

| Command 			| Parameter 												 | Information 										 |
|:---           |:--- 												 |:--- 										 |
| help      |   | Get help 									 |
There are many commands not shown here. I suggest you test them yourself
</details>

## Sponsor
RW-HPS is an open source project authorized by AGPL V3, which is free to use. However, the amount of work required to maintain and develop new features for the project is not sustainable without appropriate funding.  
Please note that sponsorship is entirely voluntary. Sponsors are not privileged and can use all features without sponsorship.

We receive donations through the following channels：
+ [爱发电](https://afdian.net/@derdct)

## Thanks
> [IntelliJ IDEA](https://zh.wikipedia.org/zh-hans/IntelliJ_IDEA) It is an IDE that maximizes the productivity of developers in all aspects, and is suitable for JVM platform languages。

[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=rw-hps)