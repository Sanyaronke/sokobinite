package com.example.sokobinite;

import android.graphics.Rect;

public class Arena {
    static public final int NORTH = 0;
    static public final int SOUTH = 1;
    static public final int WEST  = 2;
    static public final int EAST  = 3;

    static public final int UNDER_MAP= 0x000000ff;
    static public final int OVER_MAP = 0x0000ff00;
    static public final int FLOOR   = 0x00000000;
    static public final int WALL    = 0x00000001;
    static public final int GOAL    = 0x00000002;
    static public final int SOKOBAN = 0x00000100;
    static public final int CRATE   = 0x00000200;
    static public final int PLACED_CRATE = CRATE + GOAL;
    static public final int SOKOBAN_ON_GOAL = SOKOBAN + GOAL;

    private final int map_width;
    private final int map_height;
    private int player_position_x;
    private int player_position_y;
    private int moves;
    private final int[] map;
    private final Rect affected_area;

    public Arena(int w, int h) {
        map_width = w;
        map_height = h;
        player_position_x = 0;
        player_position_y = 0;
        affected_area = new Rect(0,0,0,0);
        map = new int[map_width * map_height];
    }

    public int getMapWidth() { return map_width; }
    public int getMapHeight() { return map_height; }
    public int getMoves() { return moves; }

    public String serialize() {
        StringBuilder str = new StringBuilder();
        for(int iy = 0; iy < map_height; iy++) {
            for(int ix = 0; ix < map_width; ix++) {
                int map_code = map[(iy * map_width) + ix];
                if(ix == player_position_x && iy == player_position_y) {
                    map_code = map_code | SOKOBAN;
                }
                switch(map_code) {
                    case WALL: str.append("#"); break;
                    case GOAL: str.append("."); break;
                    case CRATE: str.append("$"); break;
                    case SOKOBAN: str.append("@"); break;
                    case PLACED_CRATE: str.append("!"); break;
                    case SOKOBAN_ON_GOAL: str.append("?"); break;
                    case FLOOR: str.append(" "); break;
                    default:
                        str.append(" ");
                }
            }
            str.append("\n");
        }
        return str.toString();
    }

    public boolean moveMan(int direction) {
        switch(direction) {
            case NORTH: return tryMovingMan(0,-1);
            case EAST: return tryMovingMan(1,0);
            case WEST: return tryMovingMan(-1,0);
            case SOUTH: return tryMovingMan(0,1);
        }
        return false; // qlb
    }

    public int getTile(int x, int y) {
        int idx = y * map_width + x;
        if (x == player_position_x && y == player_position_y) {
            return SOKOBAN;
        }
        return getTileOnMap(x,y);
    }

    public int getTileOnMap(int x, int y) {
        int idx = y * map_width + x;
        if (idx >= map_width * map_height) {
            return FLOOR;
        }
        if((map[idx] & UNDER_MAP) == map[idx]) {
            return map[idx];
        } else {
            return map[idx] & OVER_MAP;
        }
    }

    public Rect lastAffectedArea() {
        return affected_area;
    }

    public boolean gameWon() {
        for(int i = 0; i < map_width * map_height; i++) {
            if((map[i] & UNDER_MAP) == GOAL && map[i] != PLACED_CRATE) {
                return false;
            }
        }
        return true;
    }

    public void setTile(int x, int y, int tile) {
        int idx = y * map_width + x;
        if (idx < map_width * map_height) {
            if ((tile & OVER_MAP) == SOKOBAN) {
                player_position_x = x;
                player_position_y = y;
                tile = tile - SOKOBAN;
            }
            map[idx] = ((tile & OVER_MAP) | map[idx] & OVER_MAP) | ((tile & UNDER_MAP) | map[idx] & UNDER_MAP);
        }
    }

    private void clearOverTile(int x, int y) {
        int idx = y * map_width + x;
        map[idx] = map[idx] & UNDER_MAP;
    }

    private boolean tryMovingMan(int dx, int dy) {
        int new_x = player_position_x + dx;
        int new_y = player_position_y + dy;
        if(validSpace(new_x, new_y, dx, dy)) {
            affected_area.set(
                    new_x > player_position_x ? player_position_x - 1: new_x - 1,
                    new_y > player_position_y ? player_position_y - 1: new_y - 1,
                    new_x < player_position_x ? player_position_x + 1: new_x + 1,
                    new_y < player_position_y ? player_position_y + 1: new_y + 1
            );
            displaceCrates(new_x, new_y, dx, dy);
            player_position_x += dx;
            player_position_y += dy;
            moves += 1;
            return true;
        }
        return false;
    }

    private boolean validSpace(int x, int y, int vx, int vy) {
        if(x >= map_width || x < 0 || y >= map_height || y < 0) {
            return false;
        }
        if(getTile(x,y) == WALL) {
            return false;
        }
        if(getTile(x,y) == CRATE) {
            int dest = getTile(x+vx,y+vy);
            if(dest != FLOOR && dest != GOAL) {
                return false;
            }
        }
        return true;
    }

    private void displaceCrates(int x, int y, int vx, int vy) {
        if(getTileOnMap(x,y) == CRATE) {
            clearOverTile(x,y);
            setTile(x+vx,y+vy,CRATE);
        }
    } 
}
