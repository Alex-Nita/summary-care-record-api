package uk.nhs.adaptors.sandbox.scr.filters;

import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static uk.nhs.adaptors.sandbox.scr.filters.consts.RequestFiltersOrder.REQUEST_ID_FILTER_ORDER;

@Configuration
@AllArgsConstructor
public class RequestIdFilterConfig {

    private final RequestIdFilter requestIdFilter;

    @Bean
    public FilterRegistrationBean<RequestIdFilter> servletRegistrationBeanForRequestID() {
        final FilterRegistrationBean<RequestIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(requestIdFilter);
        registrationBean.setOrder(REQUEST_ID_FILTER_ORDER);
        return registrationBean;
    }
}
