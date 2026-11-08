// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include "dot.hpp"
#include "player.hpp"
namespace combat {
// Three health points per four hostile contacts, accumulated per defender.
// Integer arithmetic is deterministic on host/client; no clock or extra RNG.
inline void contact(Dot& defender,int attacker,Player* players){
    defender.damageQuarter+=3;
    if(defender.damageQuarter<4)return;
    defender.damageQuarter-=4;
    if(--defender.health==0){players[defender.team].score--;players[attacker].score++;defender.team=attacker;defender.health=MAX_HEALTH/2;defender.damageQuarter=0;}
}
}
