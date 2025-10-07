# Ecconia's Chest Sorter

## For?

Minecraft Fabric 1.21.9\
Also requires the Fabric-API.

## What does it do?

On a keypress, it will sort inventories by ID, it will stack items when possible.\
Currently "generic" inventories, Shulker boxes and the player inventory main slots get sorted.

The key can be configured in the Minecraft key binding settings. Default key is 'R'.

## Why another chest sorting mod?

Because apparently all the others I found (even though they have more features and are better), need a server mod.

This mod runs on the client side only. It does not need a server mod.

## Warning, hacking?

Currently, this mod will spam slot interaction packets to the server (one for each simulated mouse-click).\
A vanilla server and Spigot do not seem to mind this.\
But if the server has an anti-cheat protection, this mod will not try to hide the automation.

**You have been warned, this mod can get you banned for packet spam!**
