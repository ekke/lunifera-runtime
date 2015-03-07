package org.lunifera.runtime.datasource.provider;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.lunifera.runtime.datasource.provider.IDataSourceService.DataSourceConfig.DsProperties;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component(name = "org.lunifera.runtime.datasource.provider", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DatasourceComponent implements
		ServiceTrackerCustomizer<DataSourceFactory, DataSourceFactory>,
		ManagedService {

	private static final String DSFACTORY_FILTER = "(&(objectClass=org.osgi.service.jdbc.DataSourceFactory)(osgi.jdbc.driver.class=%s))";
	private ComponentContext context;
	private DataSource ds;
	private XADataSource xaDs;
	private ServiceRegistration<DataSource> dsRegister;
	private ServiceRegistration<XADataSource> xaDsRegister;
	private ServiceTracker<DataSourceFactory, DataSourceFactory> dsFactoryTracker;
	private DataSourceFactory dataSourceFactory;

	@Activate
	protected void activate(ComponentContext context) {
		this.context = context;

	}

	private Filter createFilter() throws InvalidSyntaxException {
		Filter f = context.getBundleContext().createFilter(
				String.format(
						DSFACTORY_FILTER,
						context.getProperties().get(
								DataSourceFactory.OSGI_JDBC_DRIVER_NAME)));
		return f;
	}

	@Modified
	protected void update(ComponentContext context) {
		killCurrent();
		if (dataSourceFactory != null) {
			createNewDatasources(convertMapToProperties(context.getProperties()));
		}
	}

	private void killCurrent() {
		if (dsRegister != null) {
			ds = null;
			dsRegister.unregister();
			dsRegister = null;
		}

		if (xaDsRegister != null) {
			xaDs = null;
			xaDsRegister.unregister();
			xaDsRegister = null;
		}
	}

	private void createNewDatasources(Properties properties) {
		try {
			ds = dataSourceFactory.createDataSource(properties);
			dsRegister = context.getBundleContext().registerService(
					DataSource.class, ds, null);
			xaDs = dataSourceFactory.createXADataSource(properties);
			xaDsRegister = context.getBundleContext().registerService(
					XADataSource.class, xaDs, null);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	private Properties convertMapToProperties(Dictionary<String, Object> input) {
		String driverName = (String) input
				.get(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
		Properties props = new Properties();
		Enumeration<String> iter = input.keys();
		while (iter.hasMoreElements()) {
			String key = (String) iter.nextElement();
			if (filterProps(driverName).contains(key)) {
				props.setProperty(key, input.get(key).toString());
			}
		}
		return props;
	}

	private List<String> filterProps(String driverName) {
		switch (driverName) {
		case "org.apache.derby.jdbc.ClientDriver": {
			return Arrays.asList(DsProperties.values()).stream()
					.filter(x -> x.name().contains("CLIENT_DERBY"))
					.map(y -> y.toString()).collect(Collectors.toList());
		}
		case "org.apache.derby.jdbc.EmbeddedDriver": {
			return Arrays.asList(DsProperties.values()).stream()
					.filter(x -> x.name().contains("EMBEDDED_DERBY"))
					.map(y -> y.toString()).collect(Collectors.toList());
		}
		default:{
			return Arrays.asList(DsProperties.values().toString());
		}
		}
	}

	@Deactivate
	protected void deactivate(ComponentContext context) {
		this.context = null;

		killCurrent();
		dsFactoryTracker.close();
		dsFactoryTracker = null;

	}

	@Override
	public DataSourceFactory addingService(
			ServiceReference<DataSourceFactory> reference) {
		if (dataSourceFactory == null) {
			dataSourceFactory = context.getBundleContext()
					.getService(reference);
			createNewDatasources(convertMapToProperties(context.getProperties()));
			return dataSourceFactory;
		} else {
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<DataSourceFactory> reference,
			DataSourceFactory service) {

	}

	@Override
	public void removedService(ServiceReference<DataSourceFactory> reference,
			DataSourceFactory service) {
		if (service == dataSourceFactory) {
			killCurrent();
			dataSourceFactory = null;
		}
	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		try {
			dsFactoryTracker = new ServiceTracker<DataSourceFactory, DataSourceFactory>(
					context.getBundleContext(), createFilter(), this);
			dsFactoryTracker.open();
			dataSourceFactory = dsFactoryTracker.waitForService(1000);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (properties != null) {
			killCurrent();
			createNewDatasources(convertMapToProperties((Dictionary<String, Object>) properties));
		}
	}

}
