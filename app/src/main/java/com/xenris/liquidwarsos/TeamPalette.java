// SPDX-License-Identifier: GPL-3.0-or-later
// Generated from art/source/particles-1.63/palette.json; same source as the native renderer.
package com.xenris.liquidwarsos;
public final class TeamPalette {
    private TeamPalette() {}
    private static final int[] COLORS={0xff52b870,0xff4489e7,0xffe85746,0xffefe9d3,0xffedb52b,0xffaa69d5};
    public static int color(int team) {return COLORS[team>=0&&team<6?team:0];}
}
