package eu.thesociety.DragonbornSR.DragonsRadioMod;

import cpw.mods.fml.server.FMLServerHandler;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class validator
{
	public static boolean getValidity()
	{
		/* TODO gamerforEA code clear:
		if (FMLServerHandler.instance().getServer().isDedicatedServer())
			return register();
		Cmsg.writeline("Detected single player server. No restrictions enabled."); */

		return true;
	}

	public static boolean register()
	{
		/* TODO gamerforEA code replace, old code:
		try
		{
			if (getTokenFromTheSociety().equals("false"))
			{
				Cmsg.writeline("This server has no permission to use Dragon\'s Radio Mod.");
				Cmsg.writeline("Please contact info@thesociety.eu to make your copy of Dragon\'s Radio Mod legal!");
				Cmsg.writeline("Mod will not work anymore as hoped!");
				return true;
			}
			Cmsg.writeline("Detected legal Radio Mod. Thank You!");
			return true;
		}
		catch (IOException var1)
		{
			Cmsg.writeError(var1);
			return true;
		} */
		return true;
		// TODO gamerforEA code end
	}

	public static String getTokenFromTheSociety() throws IOException
	{
		String URL = "http://www.thesociety.eu/dragonsradiomod/validate.php";
		int port = FMLServerHandler.instance().getServer().getServerPort();
		URL url = new URL(URL + "?port=" + port);
		URLConnection con = url.openConnection();
		InputStream in = con.getInputStream();
		String encoding = con.getContentEncoding();
		encoding = encoding == null ? "UTF-8" : encoding;
		return IOUtils.toString(in, encoding);
	}
}
