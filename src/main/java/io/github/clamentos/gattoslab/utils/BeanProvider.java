package io.github.clamentos.gattoslab.utils;

///
import java.util.concurrent.atomic.AtomicReference;

///.
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

///
@Component

///
public class BeanProvider implements ApplicationContextAware {

    ///
    private static final AtomicReference<ApplicationContext> springContext = new AtomicReference<>();

    ///
    public static <T> T getBean(final Class<T> beanClass, final String beanName) throws BeansException {

        final ApplicationContext context = springContext.get();

        if(context == null || !context.containsBean(beanName)) return null;
        return context.getBean(beanClass);
    }

    ///.
    @Override
	public synchronized void setApplicationContext(final ApplicationContext context) {

        springContext.set(context);
	}

    ///
}
