// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/catalog-1.72.json. Never renumber surviving seed identities.
#pragma once
namespace map_catalog {
static constexpr int COUNT=59;
static constexpr int IDS[COUNT]={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59};
inline int width(int id) {return id>=50?1600:800;}
inline int height(int id) {return id>=50?960:480;}
inline int idAt(unsigned index) {return IDS[index%COUNT];}
inline int resolveExplicit(int id) {for(int active:IDS)if(id==active)return id;return IDS[0];}
}
