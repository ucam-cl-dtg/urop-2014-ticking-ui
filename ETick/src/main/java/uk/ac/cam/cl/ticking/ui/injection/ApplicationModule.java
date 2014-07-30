package uk.ac.cam.cl.ticking.ui.injection;

import uk.ac.cam.cl.dtg.teaching.exceptions.ExceptionHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.ticking.ui.api.APIOverviewResource;
import uk.ac.cam.cl.ticking.ui.api.JodaDateConfig;

import com.google.inject.Binder;
import com.google.inject.Module;

public class ApplicationModule implements Module {

	@Override
	public void configure(Binder binder) {
		binder.bind(RemoteFailureHandler.class);
		binder.bind(ExceptionHandler.class);
		binder.bind(APIOverviewResource.class);
		binder.bind(JodaDateConfig.class);
	}

}
