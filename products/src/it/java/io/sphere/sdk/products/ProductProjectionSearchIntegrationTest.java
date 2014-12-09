package io.sphere.sdk.products;

import io.sphere.sdk.attributes.*;
import io.sphere.sdk.http.ClientRequest;
import io.sphere.sdk.models.LocalizedStrings;
import io.sphere.sdk.products.commands.ProductCreateCommand;
import io.sphere.sdk.products.commands.ProductDeleteByIdCommand;
import io.sphere.sdk.products.queries.ProductProjectionQuery;
import io.sphere.sdk.products.search.ProductProjectionSearch;
import io.sphere.sdk.products.search.ProductProjectionSearchModel;
import io.sphere.sdk.producttypes.ProductTypeDraft;
import io.sphere.sdk.producttypes.ProductType;
import io.sphere.sdk.producttypes.commands.ProductTypeCreateCommand;
import io.sphere.sdk.producttypes.commands.ProductTypeDeleteByIdCommand;
import io.sphere.sdk.producttypes.queries.ProductTypeQuery;
import io.sphere.sdk.search.*;
import io.sphere.sdk.test.IntegrationTest;
import io.sphere.sdk.utils.ListUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static io.sphere.sdk.models.DefaultCurrencyUnits.EUR;
import static io.sphere.sdk.products.ProductProjectionType.STAGED;
import static io.sphere.sdk.test.SphereTestUtils.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProductProjectionSearchIntegrationTest extends IntegrationTest {
    private static final ProductProjectionSearchModel MODEL = ProductProjectionSearch.model();

    private static Product testProduct1;
    private static Product testProduct2;
    private static ProductType productType;
    public static final String TEST_CLASS_NAME = ProductProjectionSearchIntegrationTest.class.getSimpleName();
    public static final String COLOR = ("Color" + TEST_CLASS_NAME).substring(0, Math.min(25, TEST_CLASS_NAME.length()));
    public static final String SIZE = ("Size" + TEST_CLASS_NAME).substring(0, Math.min(25, TEST_CLASS_NAME.length()));
    public static final String COLOR_ATTRIBUTE_KEY = "variants.attributes." + COLOR;
    public static final String SIZE_ATTRIBUTE_KEY = "variants.attributes." + SIZE;
    public static final String PRICE_ATTRIBUTE_KEY = "variants.price.centAmount";

    @BeforeClass
    public static void setupProducts() {
        removeProducts();
        final TextAttributeDefinition colorAttributeDefinition = TextAttributeDefinitionBuilder
                .of(COLOR, LocalizedStrings.ofEnglishLocale(COLOR), TextInputHint.SingleLine).build();
        final TextAttributeDefinition sizeAttributeDefinition = TextAttributeDefinitionBuilder
                .of(SIZE, LocalizedStrings.ofEnglishLocale(SIZE), TextInputHint.SingleLine).build();

        final ProductTypeDraft productTypeDraft = ProductTypeDraft.of(TEST_CLASS_NAME, "", asList(colorAttributeDefinition, sizeAttributeDefinition));
        final ProductTypeCreateCommand productTypeCreateCommand = new ProductTypeCreateCommand(productTypeDraft);
        productType = execute(productTypeCreateCommand);
        testProduct1 = createTestProduct(productType, "Schuh", "shoe", "red", "M");
        testProduct2 = createTestProduct(productType, "Hemd", "shirt", "blue", "XL");
        final ProductProjectionSearch search = new ProductProjectionSearch(STAGED);
        execute(search, res -> {
            final List<String> ids = toIds(res.getResults());
            return ids.contains(testProduct1.getId()) && ids.contains(testProduct2.getId());
        });
        try {
            Thread.sleep(500); // Wait for elasticsearch synchronization
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Product createTestProduct(ProductType productType, String germanName, String englishName, String color, final String size) {
        final LocalizedStrings name = LocalizedStrings.of(GERMAN, germanName, ENGLISH, englishName);
        final ProductVariantDraft variant = ProductVariantDraftBuilder.of()
                .attributes(Attribute.of(COLOR, color), Attribute.of(SIZE, size))
                .price(Price.of(new BigDecimal("23.45"), EUR))
                .build();
        final ProductDraft productDraft = ProductDraftBuilder.of(productType, name, name, variant).build();
        return execute(new ProductCreateCommand(productDraft));
    }

    @AfterClass
    public static void removeProducts() {
        List<ProductType> productTypes = execute(new ProductTypeQuery().byName(TEST_CLASS_NAME)).getResults();
        if (!productTypes.isEmpty()) {
            final List<ProductProjection> products = execute(new ProductProjectionQuery(STAGED)
                    .withPredicate(ProductProjectionQuery.model().productType().isAnyOf(productTypes))).getResults();
            products.forEach(p -> execute(new ProductDeleteByIdCommand(p.toProductVersioned())));
            productTypes.forEach(p -> execute(new ProductTypeDeleteByIdCommand(p)));
        }
        testProduct1 = null;
        testProduct2 = null;
        productType = null;
    }

    @Test
    public void searchByTextInACertainLanguage() throws Exception {
        final Search<ProductProjection> search = new ProductProjectionSearch(STAGED).withText(ENGLISH, "shoe");
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        //end example parsing here
        assertThat(toIds(pagedSearchResult.getResults())).containsExactly(testProduct1.getId());
    }

    @Test
    public void sortByAnAttribute() throws Exception {
        // TODO Fix when price alias for sort is available
        final List<String> expectedAsc = asList(testProduct2.getId(), testProduct1.getId());
        MoneyAmountSearchModel<ProductProjection> amount = MODEL.variants().price().amount();
        testSorting(amount.sort(SearchSortDirection.ASC), expectedAsc);
        testSorting(amount.sort(SearchSortDirection.DESC), ListUtils.reverse(expectedAsc));
        testSorting(amount.sort(SearchSortDirection.ASC_MIN), expectedAsc);
        testSorting(amount.sort(SearchSortDirection.DESC_MAX), ListUtils.reverse(expectedAsc));
    }

    @Test
    public void responseContainsRangeFacetsForAttributes() throws Exception {
        // TODO Fix all ranges
        final FacetExpression<ProductProjection> rangeFacetExpression = MODEL.variants().price().amount().facet().allRanges();
        final Search<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(rangeFacetExpression);
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final RangeFacetResult rangeFacet = (RangeFacetResult) pagedSearchResult.getFacetsResults().get(PRICE_ATTRIBUTE_KEY);
        //end example parsing here
        assertThat(rangeFacet.getRanges().get(0).getCount()).isGreaterThan(0);
    }

    @Test
    public void responseContainsTermFacetsForAttributes() throws Exception {
        final FacetExpression<ProductProjection> termFacetExpression = MODEL.variants().attribute().ofText(COLOR).facet().allTerms();
        final Search<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(termFacetExpression);
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final TermFacetResult facetResult = (TermFacetResult) pagedSearchResult.getFacetsResults().get(COLOR_ATTRIBUTE_KEY);
        //end example parsing here
        assertThat(facetResult.getTerms().stream()
                .anyMatch(termStat -> termStat.getTerm().equals("blue") && termStat.getCount() > 0))
                .isTrue();
    }

    @Test
    public void resultsAndFacetsAreFilteredByColor() throws Exception {
        final FilterExpression<ProductProjection> filter = MODEL.variants().attribute().ofText(COLOR).filter().is("blue");
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(MODEL.variants().attribute().ofText(SIZE).facet().allTerms())
                .plusFilterResult(filter)
                .plusFilterFacet(filter);
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final TermFacetResult termFacetResult = (TermFacetResult) pagedSearchResult.getFacetsResults().get(SIZE_ATTRIBUTE_KEY);
        assertThat(termFacetResult.getTerms()).containsExactly(TermStats.of("XL", 1));
        assertThat(pagedSearchResult.size()).isEqualTo(1);
        assertThat(pagedSearchResult.getResults().get(0).getId()).isEqualTo(testProduct2.getId());
    }

    @Test
    public void onlyResultsAreFilteredByColor() throws Exception {
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(MODEL.variants().attribute().ofText(SIZE).facet().allTerms())
                .plusFilterResult(MODEL.variants().attribute().ofText(COLOR).filter().is("blue"));
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final TermFacetResult termFacetResult = (TermFacetResult) pagedSearchResult.getFacetsResults().get(SIZE_ATTRIBUTE_KEY);
        assertThat(new HashSet<>(termFacetResult.getTerms())).containsOnly(TermStats.of("XL", 1), TermStats.of("M", 1));
        assertThat(pagedSearchResult.size()).isEqualTo(1);
        assertThat(pagedSearchResult.getResults().get(0).getId()).isEqualTo(testProduct2.getId());
    }

    @Test
    public void onlyFacetsAreFilteredByColor() throws Exception {
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(MODEL.variants().attribute().ofText(SIZE).facet().allTerms())
                .plusFilterFacet(MODEL.variants().attribute().ofText(COLOR).filter().is("blue"));
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final TermFacetResult termFacetResult = (TermFacetResult) pagedSearchResult.getFacetsResults().get(SIZE_ATTRIBUTE_KEY);
        assertThat(termFacetResult.getTerms()).containsExactly(TermStats.of("XL", 1));
        final HashSet<String> ids = new HashSet<>(toIds(pagedSearchResult.getResults()));
        assertThat(ids).contains(testProduct2.getId(), testProduct1.getId());
    }

    @Test
    public void resultsArePaginated() throws Exception {
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFilterQuery(MODEL.variants().attribute().ofText(COLOR).filter().isIn(asList("red", "blue")))
                .withSort(SearchSort.of("name.en asc"))
                .withOffset(1).withLimit(1);
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        assertThat(toIds(pagedSearchResult.getResults())).containsExactly(testProduct1.getId());
    }

    @Test
    public void filterQueryFiltersBeforeFacetsAreCalculated() throws Exception {
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .plusFacet(MODEL.variants().attribute().ofText(SIZE).facet().allTerms())
                .plusFilterQuery(MODEL.variants().attribute().ofText(COLOR).filter().is("blue"));
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        assertThat(pagedSearchResult.size()).isEqualTo(1);
        assertThat(pagedSearchResult.getResults().get(0).getId()).isEqualTo(testProduct2.getId());
        final TermFacetResult termFacetResult = (TermFacetResult) pagedSearchResult.getFacetsResults().get(SIZE_ATTRIBUTE_KEY);
        assertThat(termFacetResult.getTerms()).containsExactly(TermStats.of("XL", 1));
    }

    private void testSorting(SearchSort<ProductProjection> sphereSort, List<String> expected) {
        final SearchDsl<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .withSort(sphereSort);
        final PagedSearchResult<ProductProjection> pagedSearchResult = execute(search);
        final List<String> filteredId = toIds(pagedSearchResult.getResults()).stream()
                .filter(id -> id.equals(testProduct1.getId()) || id.equals(testProduct2.getId())).collect(toList());
        assertThat(filteredId).isEqualTo(expected);
    }

    protected static <T> T execute(final ClientRequest<T> clientRequest, final Predicate<T> isOk) {
        return execute(clientRequest, 9, isOk);
    }

    protected static <T> T execute(final ClientRequest<T> clientRequest, final int attemptsLeft, final Predicate<T> isOk) {
        if (attemptsLeft < 1) {
            fail("Could not satisfy the request.");
        }

        T result = execute(clientRequest);
        if (isOk.test(result)) {
            return result;
        } else {
            LoggerFactory.getLogger(ProductProjectionSearchIntegrationTest.class).info("attempts left " + (attemptsLeft - 1));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return execute(clientRequest, attemptsLeft - 1, isOk);
        }
    }

    private void paginationExample() {
        final SearchSort<ProductProjection> sort = getASortExpressionSomeHow();
        final Search<ProductProjection> search = new ProductProjectionSearch(STAGED)
                .withSort(sort)
                .withOffset(50)
                .withLimit(25);
    }

    private SearchSort<ProductProjection> getASortExpressionSomeHow() {
        return null;
    }
}
