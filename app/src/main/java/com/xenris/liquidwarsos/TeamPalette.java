// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/particles-1.63/palette.json; same source as the native renderer.
package com.xenris.liquidwarsos;
public final class TeamPalette {
    private TeamPalette() {}
    private static final int[] COLORS={0xff52b870,0xff4489e7,0xffe85746,0xffefe9d3,0xffedb52b,0xffaa69d5};
    private static final int[] EXP={0xff21b99e,0xff579fea,0xfff17155,0xffe4dbb7,0xffe7b22f,0xffb68bdc,0xffda779e,0xff7591a9,0xff99bc4f,0xffbf855c};
    public static int color(int team) {if(GardenSession.expeditionSeats>0)return EXP[Math.max(0,Math.min(9,team))];if(GardenSession.active){if(team==0)return 0xff21b99e;if(team==2)return 0xfff17155;}return COLORS[team>=0&&team<6?team:0];}
}
