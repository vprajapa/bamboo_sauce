[#if plan.buildDefinition.customConfiguration.get('custom.sauceondemand.enabled')?has_content ]
	
	[@ui.bambooInfoDisplay title='Sauce OnDemand' float=false height='80px']
            [@ww.label label='Browser' ]
            	[@ww.param name='value']${plan.buildDefinition.customConfiguration.get('custom.sauceondemand.browser')?if_exists}[/@ww.param]
            [/@ww.label]
            [@ww.label label='Max Duration' ]
            	[@ww.param name='value']${plan.buildDefinition.customConfiguration.get('custom.sauceondemand.max-duration')?if_exists}[/@ww.param]
            [/@ww.label]
            [@ww.label label='Idle Timeout' ]
            	[@ww.param name='value']${plan.buildDefinition.customConfiguration.get('custom.sauceondemand.idle-timeout')?if_exists}[/@ww.param]
            [/@ww.label]
            [@ww.label label='Starting Browser URL' ]
            	[@ww.param name='value']${plan.buildDefinition.customConfiguration.get('custom.sauceondemand.selenium.url')?if_exists}[/@ww.param]
            [/@ww.label]
    	[/@ui.bambooInfoDisplay]
[/#if]