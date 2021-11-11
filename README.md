# ResourceManager
Java Resource Manager for server that provides cache and data loading organized xml that is parsed into Map.

## How to use

**It depends on log4j.**

The manager settings file is located in `src/main/resources/ResourceManager.properties` and looks like this

```
<ResourceManager>
    <properties>
        <cash>
            <use>true</use>
            <size>128</size>
            <max_uses>100000</max_uses>
            <min_keep_time>1000</min_keep_time>
            <quite_old_time>600000</quite_old_time>
            <clear_percent>0.15</clear_percent>
        </cash>
    </properties>
</ResourceManager>
```

Example of using just in itself

```
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

		try {
			ResourceManager.CASH_USE	  = Boolean.parseBoolean(properties.get("ResourceManager.properties.cash.use"));
			ResourceManager.CASH_SIZE	  = Integer.parseInt(properties.get("ResourceManager.properties.cash.size"));
			ResourceManager.CASH_MAX_USES	  = Integer.parseInt(properties.get("ResourceManager.properties.cash.max_uses"));
			ResourceManager.CASH_MIN_KEEP_TIME  = Integer.parseInt(properties.get("ResourceManager.properties.cash.min_keep_time"));
			ResourceManager.CASH_QUITE_OLD_TIME = Integer.parseInt(properties.get("ResourceManager.properties.cash.quite_old_time"));
			ResourceManager.CASH_CLEAR_PERCENT  = Float.parseFloat(properties.get("ResourceManager.properties.cash.clear_percent"));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}
```

You are able use `loadXmlResourceWithNoThrows` insted of `loadXmlResources`.

Also

`public static BufferedImage getImage(String imgPath) throws ClassCastException, IOException`
Tries to pull the image from the cache, if it fails, then loads it into the cache and tries again.

`public static byte[] computeChecksum(String path) throws IOException`
`public static String getMD5Checksum(String filename) throws IOException`
They can be used to check the check-sum by bytes or by a string, respectively.

`public static <T> T getClassInstance(String name) throws ClassNotFoundException`
`public static <T> T getClassInstanceWithNoThrows(String name)`
Potentially should be used for controlled loading and unloading of classes. Now this is not implemented and it is just a shorthand for loading by the system ClassLoader.

## How to develop

First of all, this is the creation of a debugged cache system. The one presented here is a purely temporary measure that helped me for a training project, but is unlikely to withstand any criticism and more importantly the traffic.

Secondary is the implementation of the inherent features, such as working with classes, creating a Tree with the Map interface, to optimize stored keys, and so on.
