import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.imageio.ImageIO;

public class Main {
	static HashMap<String, Integer> buttonPoisitionMap = new HashMap<>();
	static final String adbPath = "\\AppData\\Local\\Android\\Sdk\\platform-tools";
	static final String cPath = "C:\\Users\\";
	static final String unfriendPath = "C:\\deletedFriend\\";
	static final String unfriendScreenShotName = "unfriend";
	static final String cmd = "cmd /c ";
	static final String adbTap = cmd + "adb shell input tap ";
	static final String adbSwipe = cmd + "adb shell input swipe ";
	static final String adbScreenSize = cmd + "adb shell wm size";
	static final String tempScreenShotName = "screen.png";	
	static final String adbScreenShot = cmd + "adb exec-out screencap -p > " + tempScreenShotName;
	static final String imageSuffix = ".png";
	static final int delay = 1500;
	static int deletedIdx = 0;
	static String path;
	static Process process;
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Author: Yuxuan Liu");
		System.out.println("This program uses the Wechat payment function to check friendship status");
		System.out.println("Disclaimer:*No actual transfer of any monetary amount will occur*");
		System.out.println("Start Processing");
		path = cPath + (Boolean.parseBoolean(args[0]) ?  "rrr" :  System.getProperty("user.name")) + adbPath;
		readConfig(args[1]);
		int[] screenSize = getScreenSize();
		System.out.println("screen size " + screenSize[0] + "|" + screenSize[1]);
		processInitialList(screenSize[1], screenSize[0]);
		while(true) {
			processFollowing(screenSize[1], screenSize[0]);
		}
	}
	
	public static void processFollowing(int length, int width) throws IOException, InterruptedException {
		System.out.println("Processing Next...");
		int startX = width / 2;
		int limit = buttonPoisitionMap.get("wechatBottomY");
		int spacing = buttonPoisitionMap.get("contactSpacing");
		
		process = Runtime.getRuntime()
		        .exec(adbTap + startX + " " + (limit - 0.2 * spacing), null, new File(path));
		process.waitFor();
		checkUser(length, width, false);
		
		process = Runtime.getRuntime()
		        .exec(adbSwipe + startX + " " + (length / 2) + " " + startX + " " + ((length / 2) - 0.95 * spacing), null, new File(path));
		process.waitFor();
	}
	
	public static void checkUser(int length, int width, boolean isInitial) throws IOException, InterruptedException {
		
		Thread.sleep(delay);
		System.out.println("Entering Chat...");
		enterChat(length, width);
		Thread.sleep(delay);
		if(isInitial) {
			process = Runtime.getRuntime()
			        .exec(adbScreenShot, null, new File(path));
			System.out.println("Exec " + adbScreenShot);
			printResults(process);
			process.waitFor();
			System.out.println("Getting Image " + path + "\\" + tempScreenShotName);
			File file = new File(path + "\\" + tempScreenShotName);
	        BufferedImage image = ImageIO.read(file);
	        int i = width - 1;
	        int j = length - 1;
	        
	        int prevClr = image.getRGB(0, j);
	        for(; j > 0.8 * length; j--) {
	        	int curClr = image.getRGB(0, j);
	            if(curClr != prevClr) {
	            	buttonPoisitionMap.put("chatBottomY", j);
	            	break;
	            } else {
	            	continue;
	            }
	        }
	        
	        int y = (length + buttonPoisitionMap.get("chatBottomY")) / 2;
	        buttonPoisitionMap.put("moreY", y);
	        prevClr = image.getRGB(i, y);
	        
	        for(; i > 0.8 * width; i--) {
	        	int curClr = image.getRGB(i, y);
	            if(curClr != prevClr) {
	            	buttonPoisitionMap.put("moreX", i);
	            	break;
	            } else {
	            	continue;
	            }
	        }
	        
		}
		goToMore();
		Thread.sleep(delay);
		System.out.println("Entering Payment...");
		goToPayment();
		Thread.sleep(delay);
		enterPayAmount(length, width);
		Thread.sleep(delay);
		tapTransfer(length);
		Thread.sleep(2 * delay);
		int res = isFriend();
		if(res == 2) {
			closeTransfer();
		} else {
			System.out.println("*Unfriend Detected*");
			System.out.println("Executing Screen Capture");
			String deletedFileName = unfriendScreenShotName + deletedIdx + imageSuffix;
			System.out.println(cmd + "adb exec-out screencap -p > " + unfriendPath + deletedFileName);
			process = Runtime.getRuntime()
			        .exec(cmd + "adb exec-out screencap -p > " + unfriendScreenShotName + deletedIdx + imageSuffix, null, new File(path));
			process.waitFor();
			
			Path source = Paths.get(path + "\\" + deletedFileName);
	        Path target = Paths.get(unfriendPath);
	        Files.move(source,target.resolve(source.getFileName()), REPLACE_EXISTING);
			tapUnfriendOK(res);
		}
		back();
		back();
		goToContact();
	}
	
	public static void removeScreenShot() throws IOException, InterruptedException {
		System.out.println("Removing Temp ScreenShot...");
		System.out.println("Exec " + cmd + "del " + path + "\\" + tempScreenShotName);
		process = Runtime.getRuntime()
		        .exec(cmd + "del " + path + "\\" + tempScreenShotName, null, new File(path));
		printResults(process);
		process.waitFor();
	}
	
	public static void clearDest() throws IOException, InterruptedException {
		System.out.println("Removing Destination ScreenShot...");
		System.out.println("Exec " + cmd + "del " + unfriendPath + "\\*");
		process = Runtime.getRuntime()
		        .exec(cmd + "del " + unfriendPath + "*", null, new File(path));
		printResults(process);
		process.waitFor();
	}
	
	public static int isFriend() throws IOException, InterruptedException {
		System.out.println("Checking Friendship...");
		removeScreenShot();
		process = Runtime.getRuntime()
		        .exec(adbScreenShot, null, new File(path));
		System.out.println("Exec " + adbScreenShot);
		printResults(process);
		process.waitFor();
		System.out.println("Getting Image " + path + "\\" + tempScreenShotName);
		File file = new File(path + "\\" + tempScreenShotName);
        BufferedImage image = ImageIO.read(file);
        int extractX = buttonPoisitionMap.get("extractX");
		int extractY = buttonPoisitionMap.get("extractY");
		int extract2X = buttonPoisitionMap.get("extract2X");
		int extract2Y = buttonPoisitionMap.get("extract2Y");
        int clr = image.getRGB(extractX, extractY);
        int red =   (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue =   clr & 0x000000ff;
        int clr2 = image.getRGB(extract2X, extract2Y);
        int red2 =   (clr2 & 0x00ff0000) >> 16;
        int green2 = (clr2 & 0x0000ff00) >> 8;
        int blue2 =   clr2 & 0x000000ff;
        if(red == 12 && green == 12 && blue == 12) {
        	return 0;
        } else if (red2 == 12 && green2 == 12 && blue2 == 12) {
        	return 1;
        }
        return 2;
	}
	
	public static void enterChat(int length, int width) throws IOException, InterruptedException {
		process = Runtime.getRuntime()
		        .exec(adbScreenShot, null, new File(path));
		System.out.println("Exec " + adbScreenShot);
		printResults(process);
		process.waitFor();
		System.out.println("Getting Image " + path + "\\" + tempScreenShotName);
		File file = new File(path + "\\" + tempScreenShotName);
        BufferedImage image = ImageIO.read(file);
        int x = (int) (0.4 * width);
        for(int i = (int) (0.3 * length); i < length; i++) {
        	int clr = image.getRGB(x, i);
            int red =   (clr & 0x00ff0000) >> 16;
            int green = (clr & 0x0000ff00) >> 8;
            int blue =   clr & 0x000000ff;
            //System.out.println(red + "|" + green + "|" + blue + "|" + i);
            if(red == 125 && green == 144 && blue == 169) {
            	process = Runtime.getRuntime()
        		        .exec(adbTap + x + " " + i, null, new File(path));
        		process.waitFor();
        		break;
            }
        }
	}
	
	public static void goToContact() throws IOException, InterruptedException {
		int contactX = buttonPoisitionMap.get("contactX");
		int contactY = buttonPoisitionMap.get("contactY");
		System.out.println("Tap contact:" + contactX + "|" + contactY);
		process = Runtime.getRuntime()
		        .exec(adbTap + contactX + " " + contactY, null, new File(path));
		process.waitFor();
	}
	
	public static void closeTransfer() throws IOException, InterruptedException {
		int closeTransferX = buttonPoisitionMap.get("closeTransferX");
		int closeTransferY = buttonPoisitionMap.get("closeTransferY");

		process = Runtime.getRuntime()
		        .exec(adbTap + closeTransferX + " " + closeTransferY, null, new File(path));
		process.waitFor();
	}
	
	public static void tapUnfriendOK(int type) throws IOException, InterruptedException {
		if(type == 0) {
			int tapUnfriendX = buttonPoisitionMap.get("tapUnfriendX");
			int tapUnfriendY = buttonPoisitionMap.get("tapUnfriendY");

			process = Runtime.getRuntime()
			        .exec(adbTap + tapUnfriendX + " " + tapUnfriendY, null, new File(path));
			process.waitFor();
		} else {
			int tapUnfriend2X = buttonPoisitionMap.get("tapUnfriend2X");
			int tapUnfriend2Y = buttonPoisitionMap.get("tapUnfriend2Y");

			process = Runtime.getRuntime()
			        .exec(adbTap + tapUnfriend2X + " " + tapUnfriend2Y, null, new File(path));
			process.waitFor();
		}
		
		deletedIdx++;
	}
	
	public static void goToPayment() throws IOException, InterruptedException {
		int paymentX = buttonPoisitionMap.get("paymentX");
		int paymentY = buttonPoisitionMap.get("paymentY");

		process = Runtime.getRuntime()
		        .exec(adbTap + paymentX + " " + paymentY, null, new File(path));
		process.waitFor();
	}
	
	public static void enterPayAmount(int length, int width) throws IOException, InterruptedException {
		int numX = width / 5;
		int numY = length - ((length - buttonPoisitionMap.get("wechatBottomY")) * 4);

		process = Runtime.getRuntime()
		        .exec(adbTap + numX + " " + numY, null, new File(path));
		process.waitFor();
	}
	
	public static void tapTransfer(int length) throws IOException, InterruptedException {
		int transferX = buttonPoisitionMap.get("moreX");
		int transferY = (int) (length * 0.9);
		System.out.println("Tap transfer:" + transferX + "|" + transferY);
		process = Runtime.getRuntime()
		        .exec(adbTap + transferX + " " + transferY, null, new File(path));
		process.waitFor();
	}
	
	public static void goToMore() throws IOException, InterruptedException {
		int moreX = buttonPoisitionMap.get("moreX");
		int moreY = buttonPoisitionMap.get("moreY");
		
		process = Runtime.getRuntime()
		        .exec(adbTap + moreX + " " + moreY, null, new File(path));
		process.waitFor();
	}
	
	public static void back() throws IOException, InterruptedException {
		int backX = buttonPoisitionMap.get("backX");
		int backY = buttonPoisitionMap.get("backY");
		process = Runtime.getRuntime()
		        .exec(adbTap + backX + " " + backY, null, new File(path));
		process.waitFor();
	}
	
	public static void analyzeInitImage(int length, int width) throws IOException, InterruptedException {
		process = Runtime.getRuntime()
		        .exec(adbScreenShot, null, new File(path));
		System.out.println("Exec " + adbScreenShot);
		printResults(process);
		process.waitFor();
		System.out.println("Getting Image " + path + "\\" + tempScreenShotName);
		File file = new File(path + "\\" + tempScreenShotName);
        BufferedImage image = ImageIO.read(file);
        int x = (int) (width * 0.7);
        int i = (int) (0.3 * length);
        HashSet<Integer> colors = new HashSet<>();
        int prevClr = image.getRGB(x, i);
        colors.add(prevClr);
        for(; i < length / 2; i++) {
        	int curClr = image.getRGB(x, i);
            if(curClr != prevClr && image.getRGB(x, i + 10) == curClr) {
            	break;
            } else {
            	continue;
            }
        }
        
        prevClr = image.getRGB(x, i);
        colors.add(prevClr);
        for(; i < length / 2; i++) {
        	int curClr = image.getRGB(x, i);
            if(curClr != prevClr && image.getRGB(x, i + 10) == curClr) {
            	break;
            } else {
            	continue;
            }
        }
        
        
        prevClr = image.getRGB(x, i);
        colors.add(prevClr);
        int contactStart = i;
        buttonPoisitionMap.put("contactStartY", i);
        for(; i < length / 2; i++) {
        	int curClr = image.getRGB(x, i);
            if(curClr != prevClr) {
            	buttonPoisitionMap.put("contactSpacing", i - contactStart);
            	break;
            } else {
            	continue;
            }
        }
        
        x = (int) (width * 0.5);
        for(i = (int) (0.85 * length); i < length; i++) {
        	int curClr = image.getRGB(x, i);
            if(!colors.contains(curClr) && image.getRGB(x, i + 10) == curClr) {
            	buttonPoisitionMap.put("wechatBottomY", i);
            	break;
            }
        }
        
        i = (int) (0.25 * width);
        int contactY = (length + buttonPoisitionMap.get("wechatBottomY")) / 2;
        buttonPoisitionMap.put("contactY", contactY);
        
        prevClr = image.getRGB(i, contactY);
        for(; i < width / 2; i++) {
        	int curClr = image.getRGB(i, contactY);
            if(curClr != prevClr) {
            	buttonPoisitionMap.put("contactX", i);
            	break;
            }
        }
        System.out.println(buttonPoisitionMap);
	}
	
	public static void processInitialList(int length, int width) throws IOException, InterruptedException {
		try {
			System.out.println("Processing initial list...");
			analyzeInitImage(length, width);
			int startX = width / 2;
			int limit = buttonPoisitionMap.get("wechatBottomY");
			int curY = buttonPoisitionMap.get("contactStartY");
			int spacing = buttonPoisitionMap.get("contactSpacing");
			process = Runtime.getRuntime()
			        .exec(adbTap + startX + " " + curY, null, new File(path));
			process.waitFor();
			checkUser(length, width, true);
			curY += spacing;
			while(curY < limit) {
				process = Runtime.getRuntime()
				        .exec(adbTap + startX + " " + curY, null, new File(path));
				process.waitFor();
				checkUser(length, width, false);
				curY += spacing;
			}
			process = Runtime.getRuntime()
			        .exec(adbSwipe + startX + " " + (length / 2) + " " + startX + " " + ((length / 2) - 0.675 * spacing), null, new File(path));
			process.waitFor();
		} catch (NullPointerException e){
			System.out.println(buttonPoisitionMap);
			System.out.println("Warning: Incorrect Starting Configuration!");
			System.exit(1);
		}
	}
	
	public static int[] getScreenSize() throws IOException {
		process = Runtime.getRuntime()
		        .exec(adbScreenSize, null, new File(path));
		ArrayList<String> res = getCommandResults(process);
		int[] ret = new int[2];
		String[] splitted = res.get(0).split(" ")[2].split("x");
		ret[0] = Integer.parseInt(splitted[0]);
		ret[1] = Integer.parseInt(splitted[1]);
		return ret;
	}
	
	public static void printResults(Process process) throws IOException {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line = "";
	    while ((line = reader.readLine()) != null) {
	        System.out.println(line);
	    }
	}
	
	public static ArrayList<String> getCommandResults(Process process) throws IOException {
		ArrayList<String> res = new ArrayList<>();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line = "";
	    while ((line = reader.readLine()) != null) {
	    	res.add(line);
	    }
	    return res;
	}
	
	public static void readConfig(String model) {
		System.out.println("Reading configuration...");
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(model + ".config"));
			String line = reader.readLine();
			while (line != null) {
				String[] splitted = line.split(" ");
				buttonPoisitionMap.put(splitted[0], Integer.parseInt(splitted[1]));
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
//		________  ________  ________  ________         
//		|\   ____\|\   ____\|\   __  \|\   __  \        
//		\ \  \___|\ \  \___|\ \  \|\  \ \  \|\ /_       
//		 \ \  \____\ \  \____\ \   __  \ \   __  \      
//		  \ \  ___  \ \  ___  \ \  \ \  \ \  \|\  \     
//		   \ \_______\ \_______\ \__\ \__\ \_______\    
//		    \|_______|\|_______|\|__|\|__|\|_______|   
