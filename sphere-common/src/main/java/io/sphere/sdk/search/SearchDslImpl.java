package io.sphere.sdk.search;

import com.fasterxml.jackson.core.type.TypeReference;
import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereRequestBase;
import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.HttpQueryParameter;
import io.sphere.sdk.http.UrlQueryBuilder;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class SearchDslImpl<T> extends SphereRequestBase implements SearchDsl<T> {

    private final Optional<SearchText> text;
    private final List<FacetExpression<T>> facets;
    private final List<FilterExpression<T>> filters;
    private final List<FilterExpression<T>> filterQueries;
    private final List<FilterExpression<T>> filterFacets;
    private final List<SearchSort<T>> sort;
    private final Optional<Long> limit;
    private final Optional<Long> offset;
    private final List<HttpQueryParameter> additionalQueryParameters;
    private final Function<HttpResponse, PagedSearchResult<T>> resultMapper;
    private final String endpoint;

    public SearchDslImpl(final String endpoint, final Optional<SearchText> text, final List<FacetExpression<T>> facets,
                         final List<FilterExpression<T>> filters, final List<FilterExpression<T>> filterQueries, final List<FilterExpression<T>> filterFacets,
                         final List<SearchSort<T>> sort, final Optional<Long> limit, final Optional<Long> offset,
                         final List<HttpQueryParameter> additionalQueryParameters, Function<HttpResponse, PagedSearchResult<T>> resultMapper) {
        this.text = requireNonNull(text);
        this.facets = requireNonNull(facets);
        this.filters = requireNonNull(filters);
        this.filterQueries = requireNonNull(filterQueries);
        this.filterFacets = requireNonNull(filterFacets);
        this.sort = requireNonNull(sort);
        this.limit = requireNonNull(limit);
        this.offset = requireNonNull(offset);
        this.additionalQueryParameters = requireNonNull(additionalQueryParameters);
        this.resultMapper = requireNonNull(resultMapper);
        this.endpoint = requireNonNull(endpoint);
    }

    public SearchDslImpl(final String endpoint, final Function<HttpResponse, PagedSearchResult<T>> resultMapper,
                         final List<HttpQueryParameter> additionalQueryParameters) {
        this(endpoint, Optional.empty(), emptyList(), emptyList(), emptyList(),
                emptyList(), emptyList(), Optional.empty(), Optional.empty(), additionalQueryParameters, resultMapper);
    }

    public SearchDslImpl(final String endpoint, final TypeReference<PagedSearchResult<T>> typeReference,
                         final List<HttpQueryParameter> additionalQueryParameters) {
        this(endpoint, resultMapperOf(typeReference), additionalQueryParameters);
    }

    @Override
    public SearchDsl<T> withText(Optional<SearchText> text) {
        return copyBuilder().text(text).build();
    }

    @Override
    public SearchDsl<T> withFacets(List<FacetExpression<T>> facets) {
        return copyBuilder().facets(facets).build();
    }

    @Override
    public SearchDsl<T> withFilterResults(List<FilterExpression<T>> filterResults) {
        return copyBuilder().filterResults(filterResults).build();
    }

    @Override
    public SearchDsl<T> withFilterQuery(List<FilterExpression<T>> filterQueries) {
        return copyBuilder().filterQueries(filterQueries).build();
    }

    @Override
    public SearchDsl<T> withFilterFacets(List<FilterExpression<T>> filterFacets) {
        return copyBuilder().filterFacets(filterFacets).build();
    }

    private SearchDsl<T> withSort(List<SearchSort<T>> sort) {
        return copyBuilder().sort(sort).build();
    }

    @Override
    public SearchDsl<T> withSort(final SearchSort<T> sort) {
        return withSort(asList(sort));
    }

    @Override
    public SearchDsl<T> withLimit(final long limit) {
        return copyBuilder().limit(Optional.of(limit)).build();
    }

    @Override
    public SearchDsl<T> withOffset(long offset) {
        return copyBuilder().offset(Optional.of(offset)).build();
    }

    @Override
    public SearchDsl<T> withAdditionalQueryParameters(List<HttpQueryParameter> additionalQueryParameters) {
        return copyBuilder().additionalQueryParameters(additionalQueryParameters).build();
    }

    @Override
    public Optional<SearchText> text() {
        return text;
    }

    @Override
    public List<FacetExpression<T>> facets() {
        return facets;
    }

    @Override
    public List<FilterExpression<T>> filterResults() {
        return filters;
    }

    @Override
    public List<FilterExpression<T>> filterQueries() {
        return filterQueries;
    }

    @Override
    public List<FilterExpression<T>> filterFacets() {
        return filterFacets;
    }

    @Override
    public List<SearchSort<T>> sort() {
        return sort;
    }

    @Override
    public Optional<Long> limit() {
        return limit;
    }

    @Override
    public Optional<Long> offset() {
        return offset;
    }

    @Override
    public List<HttpQueryParameter> additionalQueryParameters() {
        return additionalQueryParameters;
    }

    @Override
    public PagedSearchResult<T> deserialize(final HttpResponse httpResponse) {
        return resultMapper.apply(httpResponse);
    }

    @Override
    public HttpRequestIntent httpRequestIntent() {
        final String additions = queryParametersToString(true);
        return HttpRequestIntent.of(HttpMethod.GET, endpoint + (additions.length() > 1 ? additions : ""));
    }

    String queryParametersToString(final boolean urlEncoded) {
        final UrlQueryBuilder builder = UrlQueryBuilder.of();
        text().ifPresent(t -> builder.add(SearchParameterKeys.TEXT + "." + t.getLocale().getLanguage(), t.getText(), urlEncoded));
        facets().forEach(f -> builder.add(SearchParameterKeys.FACET, f.toSphereFacet(), urlEncoded));
        filterResults().forEach(f -> builder.add(SearchParameterKeys.FILTER, f.toSphereFilter(), urlEncoded));
        filterQueries().forEach(f -> builder.add(SearchParameterKeys.FILTER_QUERY, f.toSphereFilter(), urlEncoded));
        filterFacets().forEach(f -> builder.add(SearchParameterKeys.FILTER_FACETS, f.toSphereFilter(), urlEncoded));
        sort().forEach(s -> builder.add(SearchParameterKeys.SORT, s.toSphereSort(), urlEncoded));
        limit().ifPresent(l -> builder.add(SearchParameterKeys.LIMIT, l.toString(), urlEncoded));
        offset().ifPresent(o -> builder.add(SearchParameterKeys.OFFSET, o.toString(), urlEncoded));
        additionalQueryParameters().forEach(p -> builder.add(p.getKey(), p.getValue(), urlEncoded));
        return builder.toStringWithOptionalQuestionMark();
    }

    @Override
    public String endpoint() {
        return endpoint;
    }

    private SearchDslBuilder<T> copyBuilder() {
        return new SearchDslBuilder<>(this);
    }

    @Override
    public String toString() {
        final String readablePath = endpoint + queryParametersToString(false);

        return "SearchDslImpl{" +
                ", text=" + text +
                ", facets=" + facets +
                ", filterResults=" + filters +
                ", filterQueries=" + filterQueries +
                ", filterFacets=" + filterFacets +
                ", sort=" + sort +
                ", limit=" + limit +
                ", offset=" + offset +
                ", additionalQueryParameters=" + additionalQueryParameters +
                ", resultMapper=" + resultMapper +
                ", readablePath=" + readablePath +
                ", endpoint='" + endpoint + '\'' +
                '}';
    }
}