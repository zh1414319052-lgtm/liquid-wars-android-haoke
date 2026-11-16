// SPDX-License-Identifier: GPL-3.0-or-later
#pragma once
#include <array>
namespace expedition_balance {
constexpr int ItemCount = 8;
constexpr std::array<int, ItemCount> CooldownMs{{35000,25000,30000,25000,35000,25000,90000,35000}};
constexpr std::array<int,4> UpgradePrice{{0,40,70,110}};
constexpr int DeathWarningMs=1000, LifeGuardMs=8000, DeathTargetIntervalMs=60000;
constexpr int DeathRange=110, MaxPopulationMultiplier=2, ReconWindowMs=60000;
constexpr int FreezeWarningMs=600,FreezeMs=3000,FreezeGraceMs=4000,CleanseMs=5000;
constexpr int AnchorMs=8000,DevourMs=8000,FortifyMs=8000,FreezeRadius=38,AnchorRadius=48,CastRange=80,TeleportRange=80;
constexpr int DevourBudget=120,ObstacleYield=30,ObstacleHp=120,DevourDamage=4,EconomyTickMs=70;
constexpr int HomeRadius=42,SightRadius=144,SiteRadius=38,Garrison=24,ContestMinimum=3,CaptureMs=4000;
constexpr int SpawnEveryMs=2000,SpawnUnits=10,AttackPercent=115;
constexpr int LootEveryMs=12000,LootLifetimeMs=90000,LootPerSeat=4,LootWavePerSeat=2,CoinValue=10,FirstDeathMs=120000;
constexpr std::array<int,4> ShoesPercent{{0,8,14,20}},ArmorPercent{{100,110,120,130}};
}
