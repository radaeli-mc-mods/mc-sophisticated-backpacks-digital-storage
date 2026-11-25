# Linking upgrades

Every remote upgrade stores its own link to one network endpoint. All nine upgrade types use the same method and work with Applied Energistics 2, Refined Storage, Tom's Simple Storage, or Integrated Dynamics with Integrated Tunnels. On NeoForge 1.21.1 with SCC 1.5.3+, or 26.1.2 with SCC 1.6.5, they can also link to Super Cool Calculator; there are no separate network-specific items.

1. Hold the remote upgrade.
2. Shift-right-click a compatible block: an ME Wireless Access Point for AE2, a Controller for Refined Storage, a Storage Terminal for Tom's Simple Storage, an Omni-Directional Connector for Integrated Dynamics, or an SCC Network Antenna on NeoForge 1.21.1 or 26.1.2.
3. Put the linked upgrade in a backpack upgrade slot and open its tab.

The item icon shows a green screen while unlinked and the network color after linking. Linking or switching networks plays a brief transition in the hotbar, in hand, and in the world. Crafting the upgrade by itself removes the link and returns directly to the green state.

To change networks, link the upgrade to another compatible endpoint. To remove the link while keeping its settings, craft the upgrade by itself. These actions require player interaction permission and network access. The addon follows the network's own availability, wireless range, and permission rules; it does not create wireless access.

An upgrade with items or fluids pending recovery cannot be relinked or unlinked until they are recovered.

Integrated Dynamics is available on Forge 1.20.1 and NeoForge 1.21.1/26.1.2. Connect an Omni-Directional Connector and the item/fluid interfaces you need. Link to the Omni or another functional part of its network; plain cables cannot be linked. No Portable Storage Terminal is required. Its linked icon is Menril cyan.

If a delivery cannot be confirmed, the upgrade keeps its pending resource record and pauses automatic retries. Relinking and unlinking stay blocked while that record exists.
