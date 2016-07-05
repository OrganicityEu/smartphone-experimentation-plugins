# Smartphone Experimentation Sensor Plugins

In order to access sensors of the Android smartphone experimenters need to implement or use Context Plugins that are executed in the experimenter's smartphones and collect data periodically.

Each plugin generates a list of measurements that are passed to the Experiment Plugins in order to be processed and generate the actual experiment results that are posted on the server.

Plugins are built using the Ambient Dynamix framework and the Open Context Plugin Sdk.

## Creating a new Plugin

The simplest way is to copy the plugin template available and simply change the name, context type and the 'Runtime' class of the plugin. 
For example in the 'NoiseLevelPlugin' you need to modify the 'AndroidManifest.xml', 'MANIFEST.MF' and source files under the 'org.ambientdynamix.contextplugins.NoiseLevelPlugin' package.

The 'PluginFactory' class is used to instantiate the plugin during runtime and actually launches the specified '*PluginRuntime' class provided by calling its 'init' method.

The 'start', 'stop' methods are used internally by the osgi manager to enable or disable the plugin. You need to to stop monitoring sensors and seize all activities of the plugin when the stop method is called in order to limit the ammount of battery consumed by your plugin.

The 'handleContextRequest' and 'handleConfiguredContextRequest' methods are used by the osgi framework when an updated sensor value is needed. Typically this is done every 30 - 60 seconds, but times may vary based on the experiment. 

## Accessing Android Sensors

For security reasons access to the Sensor and SensorManager classes is restricted. Instead you need to access the SecuredSensorManager that proxies the requests performed and limits acces to incoming requests. For example to access the Ambient Temperature Sensor of the phone you need to execute the following:

      mSensorManager = (SecuredSensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
      mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

## Generating Sensor Measurements

To generate measurements Sensor Plugins use the internal 'Reading' class. The class contains a 'context' that device the source of the measuremnt, a 'timestamp' that refers to the time instant of the measurement and a 'value' which is the string representation of a map of all the received sensor readings. A measurement can contain multiple sensor measurements like temperature, humidity and atmospheric pressure. All measurements are passed to a PluginInfo that is parsed from the osgi manager.

      //the container of the measurements
      PluginInfo info = new PluginInfo();
      //the json object that contains sensor readings
      JSONObject obj = new JSONObject();
      //adding all sensor readings to the json object
      obj.put("org.ambientdynamix.contextplugins.AmbientTemperature", temperature);
      //creating a Reading and pass it to the PluginInfo object
      r.add(new Reading(obj.toString(), PluginInfo.CONTEXT_TYPE));
      info.setPayload(r);
      info.setState("OK");
      //send the event to the osgi manager
      //the last value is the time the measurement is valid for
      sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
      
      

