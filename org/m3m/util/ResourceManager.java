package org.m3m.papermaze;

import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.imageio.ImageIO;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class ResourceManager {

	private static final String RESOURCE_MANAGER_PROPERTIES = "src/main/resources/ResourceManager.xml";
	private static int CASH_SIZE, CASH_MAX_USES, CASH_MIN_KEEP_TIME, CASH_QUITE_OLD_TIME;
	private static float CASH_CLEAR_PERCENT;
	
	private static HashMap<String, Resource<?>> cash;
	
	private static List<String> bufferList;
	
	private static void removeLessUsed() {
		if (ResourceManager.CASH_SIZE == 0)
			ResourceManager.initialize();
		
		if (cash.size() < CASH_SIZE)
			return;

		Comparator<String> sorter = (x, y) -> cash.get(x).uses > cash.get(y).uses? 1: -1;
		List<String> old = bufferList;
		old.clear();
		
		long t = System.currentTimeMillis() - CASH_QUITE_OLD_TIME;
		for (String key:cash.keySet()) {
			Resource<?> res = cash.get(key);
			if (res.lastUse < t)
				old.add(key);
		}
		if (!old.isEmpty()) {
			old.sort(sorter);
			int countToRemove = (int) (old.size() * CASH_CLEAR_PERCENT + 0.99f);
			for (int i = 0; i < countToRemove; i++)
				cash.remove(old.get(i));
			LogManager.getLogger().info("Cleared " + countToRemove + " objects from cash");
			return;
		}
		
		t = System.currentTimeMillis() - CASH_MIN_KEEP_TIME;
		for (String key:cash.keySet()) {
			Resource<?> res = cash.get(key);
			if (res.uses >= CASH_MAX_USES)
				res.uses = CASH_MAX_USES;
			if (res.lastUse > t)
				old.add(key);
		}
		if (!old.isEmpty()) {
			old.sort(sorter);
			int countToRemove = (int) (old.size() * CASH_CLEAR_PERCENT + 0.99f);
			for (int i = 0; i < countToRemove; i++)
				cash.remove(old.get(i));
			LogManager.getLogger().info("Cleared " + countToRemove + " objects from cash");
			return;
		}

		old = Arrays.asList(cash.keySet().toArray(new String[cash.size()]));
		old.sort(sorter);
		int countToRemove = (int) (old.size() * CASH_CLEAR_PERCENT + 0.9f);
		for (int i = 0; i < countToRemove; i++)
			cash.remove(old.get(i));
		LogManager.getLogger().info("Cleared " + countToRemove + " objects from cash");
	}
	
	private static void initialize() {
		Map<String, String> properties;
		try {
			properties = ResourceManager.parseXmlToHashMap(
					ResourceManager.loadXmlResource(RESOURCE_MANAGER_PROPERTIES)
				);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LogManager.getLogger().log(Level.FATAL, "Error at loading ResourceManager", e);
			throw new IllegalStateException(e);
		}

		ResourceManager.CASH_SIZE			= Integer.parseInt(properties.get("ResourceManager.properties.cash.size"));
		ResourceManager.CASH_MAX_USES		= Integer.parseInt(properties.get("ResourceManager.properties.cash.max_uses"));
		ResourceManager.CASH_MIN_KEEP_TIME	= Integer.parseInt(properties.get("ResourceManager.properties.cash.min_keep_time"));
		ResourceManager.CASH_QUITE_OLD_TIME = Integer.parseInt(properties.get("ResourceManager.properties.cash.quite_old_time"));
		ResourceManager.CASH_CLEAR_PERCENT	= Float.parseFloat(properties.get("ResourceManager.properties.cash.clear_percent"));
	}
	
	private static class Resource<T> {
		
		@SuppressWarnings("hiding")
		<T> Resource(T t) {
			this.t = t;
			lastUse = System.currentTimeMillis();
			uses++;
		}
		
		/**
		 * Last access to the resource
		 */
		long lastUse;
		/**
		 * Not more than ResourceManager.CASH_MAX_USES
		 */
		int uses;
		
		Object t;
		
		@SuppressWarnings("unchecked")
		T get() {
			lastUse = System.currentTimeMillis();
			uses++;
			return (T) t;
		}
	}

	@SuppressWarnings("unchecked")
	public static BufferedImage getImage(String imgPath) throws ClassCastException, IOException {
		try {
			Resource<BufferedImage> img = (Resource<BufferedImage>) cash.get(imgPath);
			if (img == null) {
				ResourceManager.removeLessUsed();
				Resource<BufferedImage> res = new Resource<BufferedImage>(ImageIO.read(new File(imgPath)));
				cash.put(imgPath, res);
				return res.get();
			}
			else return img.get();
		} catch (ClassCastException e) {
			LogManager.getLogger().log(Level.ERROR, e);
			throw e;
		}
	}

	public static Map<String, String> parseXmlToHashMap(Element root) {
		final Map<String, String> map = new HashMap<String, String>();
		final BiConsumer<String, Element> req = new BiConsumer<String, Element>() {
			@Override
			public void accept(String path, Element elem) {
				path += "." + elem.getTagName();
				NodeList childs = elem.getChildNodes();
				for (int i = 0; i < childs.getLength(); i++) {
					if (childs.item(i).getTextContent() != null)
						map.put(path, childs.item(i).getTextContent());
					this.accept(path, (Element) childs.item(i));
				}
		  }};
		  req.accept("", root);
		  return map;
	}

	/**
	 * 
	 * @param path
	 * @return root Element of xml tree
	 * @throws SAXException, IOException 
	 */
	public static Element loadXmlResource(String path) throws SAXException, IOException, ParserConfigurationException {
		try {
			//Get Document Builder
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			 
			//Build Document
			Document document = builder.parse(new File(path));
			 
			//Normalize the XML Structure; It's just too important !!
			document.getDocumentElement().normalize();
			 
			//Here comes the root node
			return document.getDocumentElement();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LogManager.getLogger().log(Level.ERROR, e);
			throw e;
		}
	}
	
	public static Element loadXmlResourceWithNoThrows(String path) {
		try {
			return loadXmlResource(path);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			LogManager.getLogger().log(Level.ERROR, e);
			throw new IllegalArgumentException(e);
		}
	}

	public static byte[] computeChecksum(String path) throws IOException {
		try {
			InputStream fis;
			fis = new FileInputStream(path);

		    byte[] buffer = new byte[1024];
		    MessageDigest complete = MessageDigest.getInstance("MD5");
		    int numRead;
		
		    do {
		        numRead = fis.read(buffer);
		        if (numRead > 0) {
		            complete.update(buffer, 0, numRead);
		        }
		    } while (numRead != -1);
		
		    fis.close();
		    return complete.digest();
		} catch (NoSuchAlgorithmException e) {
			LogManager.getLogger().log(Level.FATAL, e);
			throw new IllegalStateException(e);
		} catch (IOException e) {
			LogManager.getLogger().log(Level.ERROR, e);
			throw e;
		}
	}
	
	public static String getMD5Checksum(String filename) throws IOException {
	       byte[] b = computeChecksum(filename);
	       String result = "";

	       for (int i=0; i < b.length; i++)
	           result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	       return result;
	   }

	public static ClassLoader getClassLoader() {
		// TODO Auto-generated method stub
		return null;
	}
	

	@SuppressWarnings("unchecked")
	public static <T> T getClassInstance(String name) throws ClassNotFoundException {
		return (T) Class.forName(name, true, ResourceManager.getClassLoader());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getClassInstanceWithNoThrows(String name) {
		try {
			return (T) Class.forName(name, true, ResourceManager.getClassLoader());
		} catch (ClassNotFoundException e) {
			LogManager.getLogger().log(Level.ERROR, e);
			throw new IllegalArgumentException(e);
		}
	}
}
