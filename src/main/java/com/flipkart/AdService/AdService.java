package com.example.AdService;

import com.example.AdService.auth.ExampleAuthenticator;
import com.example.AdService.cli.RenderCommand;
import com.example.AdService.core.Person;
import com.example.AdService.core.Template;
import com.example.AdService.db.PersonDAO;
import com.example.AdService.health.TemplateHealthCheck;
import com.example.AdService.resources.*;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class AdService extends Application<AdServiceConfiguration> {
    public static void main(String[] args) throws Exception {
        new AdService().run(args);
    }

    private final HibernateBundle<AdServiceConfiguration> hibernateBundle =
            new HibernateBundle<AdServiceConfiguration>(Person.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(AdServiceConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "Anomaly-Detection-Service";
    }

    @Override
    public void initialize(Bootstrap<AdServiceConfiguration> bootstrap) {
        bootstrap.addCommand(new RenderCommand());
        bootstrap.addBundle(new AssetsBundle());
        bootstrap.addBundle(new MigrationsBundle<AdServiceConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(AdServiceConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(new ViewBundle());
    }

    @Override
    public void run(AdServiceConfiguration configuration,
                    Environment environment) throws ClassNotFoundException {
        final PersonDAO dao = new PersonDAO(hibernateBundle.getSessionFactory());
        final Template template = configuration.buildTemplate();

        environment.healthChecks().register("template", new TemplateHealthCheck(template));

        environment.jersey().register(new BasicAuthProvider<>(new ExampleAuthenticator(),
                                                              "SUPER SECRET STUFF"));
        environment.jersey().register(new HelloWorldResource(template));
        environment.jersey().register(new ViewResource());
        environment.jersey().register(new ProtectedResource());
        environment.jersey().register(new PeopleResource(dao));
        environment.jersey().register(new PersonResource(dao));
    }
}
