package crazypants.enderio.teleport;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.Util;
import com.enderio.core.common.vecmath.*;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.GuiHandler;
import crazypants.enderio.api.teleport.IItemOfTravel;
import crazypants.enderio.api.teleport.ITravelAccessable;
import crazypants.enderio.api.teleport.TeleportEntityEvent;
import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.config.Config;
import crazypants.enderio.enderface.TileEnderIO;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.teleport.packet.PacketDrainStaff;
import crazypants.enderio.teleport.packet.PacketOpenAuthGui;
import crazypants.enderio.teleport.packet.PacketTravelEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TravelController
{

	public static final TravelController instance = new TravelController();

	private Random rand = new Random();

	private boolean wasJumping = false;

	private boolean wasSneaking = false;

	private int delayTimer = 0;

	private int timer = Config.travelAnchorCooldown;

	private boolean tempJump;

	private boolean tempSneak;

	private boolean showTargets = false;

	public BlockCoord onBlockCoord;

	public BlockCoord selectedCoord;

	Camera currentView = new Camera();

	private final HashMap<BlockCoord, Float> candidates = new HashMap<BlockCoord, Float>();

	private boolean selectionEnabled = true;

	private double fovRad;

	private double tanFovRad;

	private final List<UniqueIdentifier> blackList = new ArrayList<GameRegistry.UniqueIdentifier>();

	private TravelController()
	{
		String[] blackListNames = Config.travelStaffBlinkBlackList;
		for (String name : blackListNames)
		{
			this.blackList.add(new UniqueIdentifier(name));
		}
	}

	public void addBlockToBlinkBlackList(String blockName)
	{
		this.blackList.add(new UniqueIdentifier(blockName));
	}

	public boolean activateTravelAccessable(ItemStack equipped, World world, EntityPlayer player, TravelSource source)
	{
		if (!this.hasTarget())
			return false;
		BlockCoord target = this.selectedCoord;
		TileEntity te = world.getTileEntity(target.x, target.y, target.z);
		if (te instanceof ITravelAccessable)
		{
			ITravelAccessable ta = (ITravelAccessable) te;
			if (ta.getRequiresPassword(player))
			{
				PacketOpenAuthGui p = new PacketOpenAuthGui(target.x, target.y, target.z);
				PacketHandler.INSTANCE.sendToServer(p);
				return true;
			}
		}
		if (this.isTargetEnderIO())
			this.openEnderIO(equipped, world, player);
		else if (Config.travelAnchorEnabled)
			this.travelToSelectedTarget(player, source, false);
		return true;
	}

	public boolean doBlink(ItemStack equipped, EntityPlayer player)
	{
		Vector3d eye = Util.getEyePositionEio(player);
		Vector3d look = Util.getLookVecEio(player);

		Vector3d sample = new Vector3d(look);
		sample.scale(Config.travelStaffMaxBlinkDistance);
		sample.add(eye);
		Vec3 eye3 = Vec3.createVectorHelper(eye.x, eye.y, eye.z);
		Vec3 end = Vec3.createVectorHelper(sample.x, sample.y, sample.z);

		double playerHeight = player.yOffset;
		//if you looking at you feet, and your player height to the max distance, or part there of
		double lookComp = -look.y * playerHeight;
		double maxDistance = Config.travelStaffMaxBlinkDistance + lookComp;

		MovingObjectPosition p = player.worldObj.rayTraceBlocks(eye3, end, !Config.travelStaffBlinkThroughClearBlocksEnabled);
		if (p == null)
		{

			//go as far as possible
			for (double i = maxDistance; i > 1; i--)
			{

				sample.set(look);
				sample.scale(i);
				sample.add(eye);
				//we test against our feets location
				sample.y -= playerHeight;
				if (this.doBlinkAround(player, sample, true))
					return true;
			}
			return false;
		}

		List<MovingObjectPosition> res = Util.raytraceAll(player.worldObj, eye3, end, !Config.travelStaffBlinkThroughClearBlocksEnabled);
		for (MovingObjectPosition pos : res)
		{
			if (pos != null)
			{
				Block hitBlock = player.worldObj.getBlock(pos.blockX, pos.blockY, pos.blockZ);
				if (this.isBlackListedBlock(player, pos, hitBlock))
					maxDistance = Math.min(maxDistance, VecmathUtil.distance(eye, new Vector3d(pos.blockX + 0.5, pos.blockY + 0.5, pos.blockZ + 0.5)) - 1.5 - lookComp);
			}
		}

		eye3 = Vec3.createVectorHelper(eye.x, eye.y, eye.z);

		Vector3d targetBc = new Vector3d(p.blockX, p.blockY, p.blockZ);
		double sampleDistance = 1.5;
		double teleDistance = VecmathUtil.distance(eye, new Vector3d(p.blockX + 0.5, p.blockY + 0.5, p.blockZ + 0.5)) + sampleDistance;

		while (teleDistance < maxDistance)
		{
			sample.set(look);
			sample.scale(sampleDistance);
			sample.add(targetBc);
			//we test against our feets location
			sample.y -= playerHeight;

			if (this.doBlinkAround(player, sample, false))
				return true;
			teleDistance++;
			sampleDistance++;
		}
		sampleDistance = -0.5;
		teleDistance = VecmathUtil.distance(eye, new Vector3d(p.blockX + 0.5, p.blockY + 0.5, p.blockZ + 0.5)) + sampleDistance;
		while (teleDistance > 1)
		{
			sample.set(look);
			sample.scale(sampleDistance);
			sample.add(targetBc);
			//we test against our feets location
			sample.y -= playerHeight;

			if (this.doBlinkAround(player, sample, false))
				return true;
			sampleDistance--;
			teleDistance--;
		}
		return false;
	}

	private boolean isBlackListedBlock(EntityPlayer player, MovingObjectPosition pos, Block hitBlock)
	{
		UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(hitBlock);
		if (ui == null)
			return false;
		return this.blackList.contains(ui) && (hitBlock.getBlockHardness(player.worldObj, pos.blockX, pos.blockY, pos.blockZ) < 0 || !Config.travelStaffBlinkThroughUnbreakableBlocksEnabled);
	}

	private boolean doBlinkAround(EntityPlayer player, Vector3d sample, boolean conserveMomentum)
	{
		if (this.doBlink(player, new BlockCoord((int) Math.floor(sample.x), (int) Math.floor(sample.y) - 1, (int) Math.floor(sample.z)), conserveMomentum))
			return true;
		if (this.doBlink(player, new BlockCoord((int) Math.floor(sample.x), (int) Math.floor(sample.y), (int) Math.floor(sample.z)), conserveMomentum))
			return true;
		return this.doBlink(player, new BlockCoord((int) Math.floor(sample.x), (int) Math.floor(sample.y) + 1, (int) Math.floor(sample.z)), conserveMomentum);
	}

	private boolean doBlink(EntityPlayer player, BlockCoord coord, boolean conserveMomentum)
	{
		return this.travelToLocation(player, TravelSource.STAFF_BLINK, coord, conserveMomentum);
	}

	public boolean showTargets()
	{
		return this.showTargets && this.selectionEnabled;
	}

	public void setSelectionEnabled(boolean b)
	{
		this.selectionEnabled = b;
		if (!this.selectionEnabled)
			this.candidates.clear();
	}

	public boolean isBlockSelected(BlockCoord coord)
	{
		if (coord == null)
			return false;
		return coord.equals(this.selectedCoord);
	}

	public void addCandidate(BlockCoord coord)
	{
		if (!this.candidates.containsKey(coord))
			this.candidates.put(coord, -1f);
	}

	public int getMaxTravelDistanceSq()
	{
		return TravelSource.getMaxDistanceSq();
	}

	public boolean isTargetEnderIO()
	{
		if (this.selectedCoord == null)
			return false;
		return EnderIO.proxy.getClientPlayer().worldObj.getBlock(this.selectedCoord.x, this.selectedCoord.y, this.selectedCoord.z) == EnderIO.blockEnderIo;
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onRender(RenderWorldLastEvent event)
	{

		Minecraft mc = Minecraft.getMinecraft();
		Vector3d eye = Util.getEyePositionEio(mc.thePlayer);
		Vector3d lookAt = Util.getLookVecEio(mc.thePlayer);
		lookAt.add(eye);
		Matrix4d mv = VecmathUtil.createMatrixAsLookAt(eye, lookAt, new Vector3d(0, 1, 0));

		float fov = Minecraft.getMinecraft().gameSettings.fovSetting;
		Matrix4d pr = VecmathUtil.createProjectionMatrixAsPerspective(fov, 0.05f, mc.gameSettings.renderDistanceChunks * 16, mc.displayWidth, mc.displayHeight);
		this.currentView.setProjectionMatrix(pr);
		this.currentView.setViewMatrix(mv);
		this.currentView.setViewport(0, 0, mc.displayWidth, mc.displayHeight);

		this.fovRad = Math.toRadians(fov);
		this.tanFovRad = Math.tanh(this.fovRad);
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.END)
		{
			EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
			if (player == null)
				return;
			this.onBlockCoord = this.getActiveTravelBlock(player);
			boolean onBlock = this.onBlockCoord != null;
			this.showTargets = onBlock || this.isTravelItemActive(player);
			if (this.showTargets)
				this.updateSelectedTarget(player);
			else
				this.selectedCoord = null;
			MovementInput input = player.movementInput;
			this.tempJump = input.jump;
			this.tempSneak = input.sneak;

			// Handles teleportation if a target is selected
			if (input.jump && !this.wasJumping && onBlock && this.selectedCoord != null && this.delayTimer == 0 || input.sneak && !this.wasSneaking && onBlock && this.selectedCoord != null && this.delayTimer == 0 && Config.travelAnchorSneak)
			{

				this.onInput(player);
				this.delayTimer = this.timer;
			}
			// If there is no selected coordinate and the input is jump, go up
			if (input.jump && !this.wasJumping && onBlock && this.selectedCoord == null && this.delayTimer == 0)
			{

				this.updateVerticalTarget(player, 1);
				this.onInput(player);
				this.delayTimer = this.timer;

			}

			// If there is no selected coordinate and the input is sneak, go down
			if (input.sneak && !this.wasSneaking && onBlock && this.selectedCoord == null && this.delayTimer == 0)
			{
				this.updateVerticalTarget(player, -1);
				this.onInput(player);
				this.delayTimer = this.timer;
			}

			if (this.delayTimer != 0)
				this.delayTimer--;

			this.wasJumping = this.tempJump;
			this.wasSneaking = this.tempSneak;
			this.candidates.clear();
		}
	}

	public boolean hasTarget()
	{
		return this.selectedCoord != null;
	}

	public void openEnderIO(ItemStack equipped, World world, EntityPlayer player)
	{
		BlockCoord target = TravelController.instance.selectedCoord;
		TileEntity te = world.getTileEntity(target.x, target.y, target.z);
		if (!(te instanceof TileEnderIO))
			return;
		TileEnderIO eio = (TileEnderIO) te;
		if (eio.canBlockBeAccessed(player))
		{
			int requiredPower = equipped == null ? 0 : instance.getRequiredPower(player, TravelSource.STAFF, target);
			if (requiredPower <= 0 || requiredPower <= this.getEnergyInTravelItem(equipped))
			{
				if (requiredPower > 0)
				{
					PacketDrainStaff p = new PacketDrainStaff(requiredPower);
					PacketHandler.INSTANCE.sendToServer(p);
				}
				player.openGui(EnderIO.instance, GuiHandler.GUI_ID_ENDERFACE, world, target.x, TravelController.instance.selectedCoord.y, TravelController.instance.selectedCoord.z);
			}
		}
		else
			player.addChatComponentMessage(new ChatComponentTranslation("enderio.gui.travelAccessable.unauthorised"));
	}

	public int getEnergyInTravelItem(ItemStack equipped)
	{
		if (equipped == null || !(equipped.getItem() instanceof IItemOfTravel))
			return 0;
		return ((IItemOfTravel) equipped.getItem()).getEnergyStored(equipped);
	}

	public boolean isTravelItemActive(EntityPlayer ep)
	{
		if (ep == null || ep.getCurrentEquippedItem() == null)
			return false;
		ItemStack equipped = ep.getCurrentEquippedItem();
		if (equipped.getItem() instanceof IItemOfTravel)
			return ((IItemOfTravel) equipped.getItem()).isActive(ep, equipped);
		return false;
	}

	public boolean travelToSelectedTarget(EntityPlayer player, TravelSource source, boolean conserveMomentum)
	{
		return this.travelToLocation(player, source, this.selectedCoord, conserveMomentum);
	}

	public boolean travelToLocation(EntityPlayer player, TravelSource source, BlockCoord coord, boolean conserveMomentum)
	{

		if (source != TravelSource.STAFF_BLINK)
		{
			TileEntity te = player.worldObj.getTileEntity(coord.x, coord.y, coord.z);
			if (te instanceof ITravelAccessable)
			{
				ITravelAccessable ta = (ITravelAccessable) te;
				if (!ta.canBlockBeAccessed(player))
				{
					player.addChatComponentMessage(new ChatComponentTranslation("enderio.gui.travelAccessable.unauthorised"));
					return false;
				}
			}
		}

		int requiredPower = 0;
		requiredPower = this.getRequiredPower(player, source, coord);
		if (requiredPower < 0)
			return false;

		if (!this.isInRangeTarget(player, coord, source.getMaxDistanceTravelledSq()))
		{
			if (source != TravelSource.STAFF_BLINK)
				player.addChatComponentMessage(new ChatComponentTranslation("enderio.blockTravelPlatform.outOfRange"));
			return false;
		}
		if (!this.isValidTarget(player, coord, source))
		{
			if (source != TravelSource.STAFF_BLINK)
				player.addChatComponentMessage(new ChatComponentTranslation("enderio.blockTravelPlatform.invalidTarget"));
			return false;
		}
		if (this.doClientTeleport(player, coord, source, requiredPower, conserveMomentum))
			for (int i = 0; i < 6; ++i)
			{
				player.worldObj.spawnParticle("portal", player.posX + (this.rand.nextDouble() - 0.5D), player.posY + this.rand.nextDouble() * player.height - 0.25D, player.posZ + (this.rand.nextDouble() - 0.5D), (this.rand.nextDouble() - 0.5D) * 2.0D, -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 2.0D);
			}
		return true;
	}

	public int getRequiredPower(EntityPlayer player, TravelSource source, BlockCoord coord)
	{
		if (!this.isTravelItemActive(player))
			return 0;
		int requiredPower;
		ItemStack staff = player.getCurrentEquippedItem();
		requiredPower = (int) (this.getDistance(player, coord) * source.getPowerCostPerBlockTraveledRF());
		int canUsePower = this.getEnergyInTravelItem(staff);
		if (requiredPower > canUsePower)
		{
			player.addChatComponentMessage(new ChatComponentTranslation("enderio.itemTravelStaff.notEnoughPower"));
			return -1;
		}
		return requiredPower;
	}

	// TODO gamerforEA code replace, old code:
	// private boolean isInRangeTarget(EntityPlayer player, BlockCoord bc, float maxSq)
	public boolean isInRangeTarget(EntityPlayer player, BlockCoord bc, float maxSq)
	// TODO gamerforEA code end
	{
		return this.getDistanceSquared(player, bc) <= maxSq;
	}

	private double getDistanceSquared(EntityPlayer player, BlockCoord bc)
	{
		if (player == null || bc == null)
			return 0;
		Vector3d eye = Util.getEyePositionEio(player);
		Vector3d target = new Vector3d(bc.x + 0.5, bc.y + 0.5, bc.z + 0.5);
		return eye.distanceSquared(target);
	}

	private double getDistance(EntityPlayer player, BlockCoord coord)
	{
		return Math.sqrt(this.getDistanceSquared(player, coord));
	}

	// TODO gamerforEA code replace, old code:
	// private boolean isValidTarget(EntityPlayer player, BlockCoord bc, TravelSource source)
	public boolean isValidTarget(EntityPlayer player, BlockCoord bc, TravelSource source)
	// TODO gamerforEA code end
	{
		if (bc == null)
			return false;
		World w = player.worldObj;
		BlockCoord baseLoc = bc;
		//targeting a block so go one up
		if (source != TravelSource.STAFF_BLINK)
			baseLoc = bc.getLocation(ForgeDirection.UP);

		return this.canTeleportTo(player, source, baseLoc, w) && this.canTeleportTo(player, source, baseLoc.getLocation(ForgeDirection.UP), w);
	}

	private boolean canTeleportTo(EntityPlayer player, TravelSource source, BlockCoord bc, World w)
	{
		if (bc.y < 1)
			return false;
		if (source == TravelSource.STAFF_BLINK && !Config.travelStaffBlinkThroughSolidBlocksEnabled)
		{
			Vec3 start = Util.getEyePosition(player);
			Vec3 target = Vec3.createVectorHelper(bc.x + 0.5f, bc.y + 0.5f, bc.z + 0.5f);
			if (!this.canBlinkTo(bc, w, start, target))
				return false;
		}

		Block block = w.getBlock(bc.x, bc.y, bc.z);
		if (block == null || block.isAir(w, bc.x, bc.y, bc.z))
			return true;
		final AxisAlignedBB aabb = block.getCollisionBoundingBoxFromPool(w, bc.x, bc.y, bc.z);
		return aabb == null || aabb.getAverageEdgeLength() < 0.7;
	}

	private boolean canBlinkTo(BlockCoord bc, World w, Vec3 start, Vec3 target)
	{
		MovingObjectPosition p = w.rayTraceBlocks(start, target, !Config.travelStaffBlinkThroughClearBlocksEnabled);
		if (p != null)
		{
			if (!Config.travelStaffBlinkThroughClearBlocksEnabled)
				return false;
			Block block = w.getBlock(p.blockX, p.blockY, p.blockZ);
			if (this.isClear(w, block, p.blockX, p.blockY, p.blockZ))
			{
				if (new BlockCoord(p.blockX, p.blockY, p.blockZ).equals(bc))
					return true;
				//need to step
				Vector3d sv = new Vector3d(start.xCoord, start.yCoord, start.zCoord);
				Vector3d rayDir = new Vector3d(target.xCoord, target.yCoord, target.zCoord);
				rayDir.sub(sv);
				rayDir.normalize();
				rayDir.add(sv);
				return this.canBlinkTo(bc, w, Vec3.createVectorHelper(rayDir.x, rayDir.y, rayDir.z), target);

			}
			return false;
		}
		return true;
	}

	private boolean isClear(World w, Block block, int x, int y, int z)
	{
		if (block == null || block.isAir(w, x, y, z))
			return true;
		final AxisAlignedBB aabb = block.getCollisionBoundingBoxFromPool(w, x, y, z);
		if (aabb == null || aabb.getAverageEdgeLength() < 0.7)
			return true;

		return block.getLightOpacity(w, x, y, z) < 2;
	}

	@SideOnly(Side.CLIENT)
	private void updateVerticalTarget(EntityPlayer player, int direction)
	{

		BlockCoord currentBlock = this.getActiveTravelBlock(player);
		World world = Minecraft.getMinecraft().theWorld;
		for (int i = 0, y = currentBlock.y + direction; i < Config.travelAnchorMaxDistance && y >= 0 && y <= 255; i++, y += direction)
		{

			//Circumvents the raytracing used to find candidates on the y axis
			TileEntity selectedBlock = world.getTileEntity(currentBlock.x, y, currentBlock.z);

			if (selectedBlock instanceof ITravelAccessable)
			{
				ITravelAccessable travelBlock = (ITravelAccessable) selectedBlock;
				BlockCoord targetBlock = new BlockCoord(currentBlock.x, y, currentBlock.z);

				if (Config.travelAnchorSkipWarning)
				{
					if (travelBlock.getRequiresPassword(player))
						player.addChatComponentMessage(new ChatComponentTranslation("enderio.gui.travelAccessable.skipLocked"));

					if (travelBlock.getAccessMode() == ITravelAccessable.AccessMode.PRIVATE && !travelBlock.canUiBeAccessed(player))
						player.addChatComponentMessage(new ChatComponentTranslation("enderio.gui.travelAccessable.skipPrivate"));
					if (!this.isValidTarget(player, targetBlock, TravelSource.BLOCK))
						player.addChatComponentMessage(new ChatComponentTranslation("enderio.gui.travelAccessable.skipObstructed"));
				}
				if (travelBlock.canBlockBeAccessed(player) && this.isValidTarget(player, targetBlock, TravelSource.BLOCK))
				{
					this.selectedCoord = targetBlock;
					return;
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private void updateSelectedTarget(EntityPlayer player)
	{
		this.selectedCoord = null;
		if (this.candidates.isEmpty())
			return;

		double closestDistance = Double.MAX_VALUE;
		for (BlockCoord bc : this.candidates.keySet())
		{
			if (!bc.equals(this.onBlockCoord))
			{

				double d = this.addRatio(bc);
				if (d < closestDistance)
				{
					this.selectedCoord = bc;
					closestDistance = d;
				}
			}
		}

		if (this.selectedCoord != null)
		{

			Vector3d blockCenter = new Vector3d(this.selectedCoord.x + 0.5, this.selectedCoord.y + 0.5, this.selectedCoord.z + 0.5);
			Vector2d blockCenterPixel = this.currentView.getScreenPoint(blockCenter);

			Vector2d screenMidPixel = new Vector2d(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
			screenMidPixel.scale(0.5);

			double pixDist = blockCenterPixel.distance(screenMidPixel);
			double rat = pixDist / Minecraft.getMinecraft().displayHeight;
			if (rat != rat)
				rat = 0;
			if (rat > 0.07)
				this.selectedCoord = null;

		}
	}

	@SideOnly(Side.CLIENT)
	private void onInput(EntityClientPlayerMP player)
	{

		MovementInput input = player.movementInput;
		BlockCoord target = TravelController.instance.selectedCoord;
		if (target == null)
			return;

		TileEntity te = player.worldObj.getTileEntity(target.x, target.y, target.z);
		if (te instanceof ITravelAccessable)
		{
			ITravelAccessable ta = (ITravelAccessable) te;
			if (ta.getRequiresPassword(player))
			{
				PacketOpenAuthGui p = new PacketOpenAuthGui(target.x, target.y, target.z);
				PacketHandler.INSTANCE.sendToServer(p);
				return;
			}
		}

		if (this.isTargetEnderIO())
			this.openEnderIO(null, player.worldObj, player);
		else if (Config.travelAnchorEnabled && this.travelToSelectedTarget(player, TravelSource.BLOCK, false))
		{
			input.jump = false;
			try
			{
				ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, player, 0, "flyToggleTimer", "field_71101_bC");
			}
			catch (Exception e)
			{
				//ignore
			}
		}

	}

	public double getScaleForCandidate(Vector3d loc)
	{

		if (!this.currentView.isValid())
			return 1;

		BlockCoord bc = new BlockCoord(loc.x, loc.y, loc.z);
		float ratio = -1;
		Float r = this.candidates.get(bc);
		if (r != null)
			ratio = r;
		if (ratio < 0)
		{
			//no cached value
			this.addRatio(bc);
			ratio = this.candidates.get(bc);
		}

		//smoothly zoom to a larger size, starting when the point is the middle 20% of the screen
		float start = 0.2f;
		float end = 0.01f;
		double mix = MathHelper.clamp_float((start - ratio) / (start - end), 0, 1);
		double scale = 1;
		if (mix > 0)
		{

			Vector3d eyePoint = Util.getEyePositionEio(EnderIO.proxy.getClientPlayer());
			scale = this.tanFovRad * eyePoint.distance(loc);

			//Using this scale will give us the block full screen, we will make it 20% of the screen
			scale *= Config.travelAnchorZoomScale;

			//only apply 70% of the scaling so more distance targets are still smaller than closer targets
			float nf = 1 - MathHelper.clamp_float((float) eyePoint.distanceSquared(loc) / TravelSource.STAFF.getMaxDistanceTravelledSq(), 0, 1);
			scale = scale * (0.3 + 0.7 * nf);

			scale = scale * mix + (1 - mix);
			scale = Math.max(1, scale);

		}
		return scale;
	}

	@SideOnly(Side.CLIENT)
	private double addRatio(BlockCoord bc)
	{
		Vector2d sp = this.currentView.getScreenPoint(new Vector3d(bc.x + 0.5, bc.y + 0.5, bc.z + 0.5));
		Vector2d mid = new Vector2d(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
		mid.scale(0.5);
		double d = sp.distance(mid);
		if (d != d)
			d = 0f;
		float ratio = (float) d / Minecraft.getMinecraft().displayWidth;
		this.candidates.put(bc, ratio);
		return d;
	}

	@SideOnly(Side.CLIENT)
	private int getMaxTravelDistanceSqForPlayer(EntityPlayer player)
	{
		if (this.isTravelItemActive(player))
			return TravelSource.STAFF.getMaxDistanceTravelledSq();
		return TravelSource.BLOCK.getMaxDistanceTravelledSq();
	}

	@SideOnly(Side.CLIENT)
	public boolean doClientTeleport(Entity entity, BlockCoord bc, TravelSource source, int powerUse, boolean conserveMomentum)
	{

		TeleportEntityEvent evt = new TeleportEntityEvent(entity, source, bc.x, bc.y, bc.z);
		if (MinecraftForge.EVENT_BUS.post(evt))
			return false;

		bc = new BlockCoord(evt.targetX, evt.targetY, evt.targetZ);
		PacketTravelEvent p = new PacketTravelEvent(entity, bc.x, bc.y, bc.z, powerUse, conserveMomentum, source);
		PacketHandler.INSTANCE.sendToServer(p);
		return true;
	}

	@SideOnly(Side.CLIENT)
	private BlockCoord getActiveTravelBlock(EntityPlayer player)
	{
		World world = Minecraft.getMinecraft().theWorld;
		if (world != null && player != null)
		{
			int x = MathHelper.floor_double(player.posX);
			int y = MathHelper.floor_double(player.boundingBox.minY) - 1;
			int z = MathHelper.floor_double(player.posZ);
			if (world.getBlock(x, y, z) == EnderIO.blockTravelPlatform)
				return new BlockCoord(x, y, z);
		}
		return null;
	}

}
