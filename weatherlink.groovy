/**
 *  Weather Link Weather Driver - for Local Device 
 * 
 *  Copyright 2021 Dale Munn
 *
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 */
//import groovy.json.JsonOutput

metadata {
    definition (name: "Weather Link Live Local", namespace: "okdale", author: "Dale Munn") {
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Water Sensor"
        capability "Switch"
        capability "Refresh"
 //       command "PollStation"
  //      command "poll"

        attribute "wind direction", "number"
        attribute "wind chill", "number"
        attribute "wind speed", "number"
        attribute "dew point", "number"
        attribute "heat index", "number"
        attribute "RainForPeriod", "number"
        attribute "rainfall for period", "number"
   			attribute "rainfall last 24 hr", "number"
        attribute "rainfall_monthly", "number"
        attribute "Solar", "number"
        attribute "UV index", "number"
        attribute "hum_in", "number"
        attribute "temp_in", "number"
        attribute "wind Gust", "number"
        attribute "pressure", "number"

    }
    preferences() {

        section("Query Inputs"){
            input "ipaddress", "text", required: true, title: "Weather Link Live IP/URI", defaultValue: "0.0.0.0"
            input "wlinkPort", "text", required: true, title: "Connection Port", defaultValue: "80"
            input "wlinkPath", "text", required: true, title: "path to file", defaultValue: "v1/current_conditions"
            //input "amtRain", "text", required: false, title: "amout of rain required to show as wet", defaultValue: ".25"
						input "txid", "number", required: true, title: "Transmitter ID", defaultValue: 1
            input "logSet", "bool", title: "Log Debug", required: true, defaultValue: false
            input "logInfo", "bool", title: "Log Info", required: true, defaultValue: false
           input "pollInterval", "enum", title: "Weather Link Live Poll Interval", required: true, defaultValue: "5 Minutes", options: ["Manual Poll Only", "5 Minutes", "10 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "3 Hours"]
        }

    }
}

def initialize(){
	updated()

}
def updated() {
    logCheck()
    LOGDEBUG("Updated called")


    PollStation()
    def pollIntervalCmd = (settings?.pollInterval ?: "3 Hours").replace(" ", "")

    unschedule()
    
    //log.debug ("${pollIntervalCmd} ${pollInterval}")
    if(pollInterval == "Manual Poll Only"){LOGINFO( "MANUAL POLLING ONLY")}
    else{ "runEvery${pollIntervalCmd}"(pollSchedule)}
    state.Interval = pollInterval

}

void refresh() {
    LOGINFO("Refresh")
	PollStation()
}
def poll(){
    log.info "Manual Poll"
    PollStation()
}



def parse(String description) {
}

def toIntOrNull = { it?.isInteger() ? it.toInteger() : null }

 
def PollStation()
{
    LOGINFO("WeatherLink: PollStation called")
    def params = [
        uri: "http://${ipaddress}:${wlinkPort}/${wlinkPath}", timeout: 5
         ]

    try {
        httpGet(params) { resp ->
           resp.headers.each {
           LOGINFO( "Response1: ${it.name} : ${it.value}")
        }
    if(logSet == true){
       LOGINFO( "params1: ${params1}")
       LOGINFO( "response contentType: ${resp.contentType}")
 		   LOGINFO( "response data: ${resp.data}")
  		 LOGINFO( "response data: ${resp.data.data.conditions}")
		   LOGINFO( "response data: ${resp.data.data.conditions.temp}")
		   LOGINFO( "response dew_point: ${resp.data.data.conditions.dew_point}")
		   LOGINFO( "response conditions: ${resp.data.data.conditions}")
       }
           // def conditions
          //  conditions = resp.data.data.conditions
           // LOGINFO("conditions: ${conditions}")
    def rain, wet
    def conditions = resp.data.data.conditions
    def txid_present = false
    for (item in conditions) {
        if(item.txid == txid) {
        	txid_present = true
          state.conditions = item
          state.time = now
          def  rainfall_last_24_hr = item.rainfall_last_24_hr * 0.01g
         	def rainfall_last_15_min = item.rainfall_last_15_min / 100.0
          send_Event(name:"Solar", value: item.solar_rad, unit: "W/m*m", descriptionText: "Solar Radiation")
					send_Event(name:"UV index", value: item.uv_index, descriptionText: "UV Index")
					send_Event(name:"temperature", value: item.temp, unit: "°F")
 					send_Event(name:"dew point", value: item.dew_point, unit: "°F")
  				send_Event(name:"heat index", value: item.heat_index, unit: "°F")
 					send_Event(name:"wind chill", value: item.wind_chill, unit: "°F")
          send_Event(name:"humidity", value: item.hum, unit: "%")
          send_Event(name:"wind speed", value: item.wind_speed_hi_last_2_min, unit: "MPH", descriptionText: "Hi Wind Speed Last 2 Min")
          send_Event(name:"wind Gust", value: item.wind_speed_hi_last_10_min, unit: "MPH", descriptionText: "Hi Wind Speed Last 10 Min")
          send_Event(name:"wind direction", value: item.wind_dir_at_hi_speed_last_10_min, unit: "°", descriptionText: "Wind Direction at hi speed 10 min")
          send_Event(name:"rainfall last 24 hr", value: rainfall_last_24_hr, unit: "in")
          send_Event(name:"rainfall_monthly", value: item.rainfall_monthly * 0.01g)
          if(send_Event(name: "RainForPeriod", value: rainfall_last_15_min, unit:"in", descriptionText: "Rainfall for the last 15 minutes")) {
                 wet =  0
          			rain = rainfall_last_15_min
           			if (rain > 0.1) {
           				wet = 1 }
  
            		if (wet == 1) {
                    send_Event(name:"water", value:"wet")
                    send_Event(name:"switch", value:"on")
            			} else {
                    send_Event(name:"water", value:"dry")
                    send_Event(name:"switch", value:"off")               
            			}
            	}
            }
          else if (item.temp_in != null) {
          		send_Event(name:"temp_in", value: item.temp_in, unit: "°F")
          		send_Event(name:"hum_in", value: item.hum_in, unit: "%", descriptionText: "Inside Humidity")
						}
					else if (item.bar_sea_level != null) {
							send_Event(name:"pressure", value: item.bar_sea_level, unit: "inHg", descriptionText: "Barometrin Pressure")
							}
          }
        }
      if(txid_present == false) {
      	log.error("Trandsmitter Id ${txid} not reporting")
      	}

    } catch (e) {
        log.error "something went wrong: $e"
    }

}

def pollSchedule() {
    LOGINFO("Scheduled Poll")
    PollStation()
}

def logCheck() {
    state.LogDebug = logSet
    if(state.LogDebug == true){
        log.info "Debug Logging Enabled"
    } else if(state.checkLog == false){
        log.info "Debug Logging Disabled"
    }
    state.LogInfo = logInfo
    if(state.LogInfo == true){
        log.info "Info Logging Enabled"
    } else if(state.checkLog == false){
        log.info "Info Logging Disabled"
    }
}

def on() {
    PollStation()
}

def off() {
    PollStation()
}

def LOGDEBUG(txt){
    try {
    	if(state.LogDebug == true){ log.debug("wlink Driver - DEBUG:  ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}

def LOGINFO(txt){
    try {
    	if(state.LogInfo == true){log.info("wlink Driver - INFO:  ${txt}") }
    } catch(ex) {
    	log.error("LOGINFO unable to output requested data!")
    }
}
private set_Event(String attribute, def newvalue) {
    if (device.currentValue(attribute) != newvalue) {
    //def currentValue = device.currentValue(attribute)
    //log.debug("$currentValue, $attribute,$newvalue")
        sendEvent(name: attribute, value: newvalue, isStateChange: true)
    }
}

private send_Event (evt) {
	if(device.currentValue(evt.name) != evt.value) {
	//	log.info("name:$evt.name, current value: ${device.currentValue(evt.name)}, value:$evt.value")
			sendEvent(evt)
			return true
			}
	return false
}
		
