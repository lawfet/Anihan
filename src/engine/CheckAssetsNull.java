package engine;

import managers.AssetManager;

public class CheckAssetsNull {
    public static void main(String[] args) {
        AssetManager am = AssetManager.getInstance();
        System.out.println("tileset is null? " + (am.getTileset() == null));
        System.out.println("chicken is null? " + (am.getChickenFrames() == null));
        System.out.println("soilDry is null? " + (am.getSoilDry() == null));
        System.out.println("treeFrames is null? " + (am.getTreeFrames() == null));
        System.out.println("baseSingle is null? " + (am.getBaseSingle() == null));
        System.out.println("baseAnims size: " + (am.getBaseFrame(AssetManager.Action.IDLE, 0) == null));
        System.out.println("statBars is null? " + (am.getStatBars() == null));
        System.out.println("menuBoard is null? " + (am.getMenuBoard() == null));
    }
}
