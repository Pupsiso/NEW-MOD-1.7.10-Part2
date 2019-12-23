package eu.thesociety.DragonbornSR.DragonsRadioMod.misc;

import net.minecraft.world.World;

public class Speaker
{
	public double x;
	public double y;
	public double z;
	public World world;

	public Speaker(double x, double y, double z, World w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = w;
	}

	// TODO gamerforEA code start
	public boolean isSamePos(int x, int y, int z)
	{
		return (int) this.x == x && (int) this.y == y && (int) this.z == z;
	}
	// TODO gamerforEA code end
}
