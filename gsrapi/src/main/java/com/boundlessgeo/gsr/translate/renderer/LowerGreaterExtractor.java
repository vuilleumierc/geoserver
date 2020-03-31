/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.translate.renderer;

import org.geotools.util.NumberRange;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

import java.util.List;

class LowerGreaterExtractor implements PropertyRangeExtractor {

    @Override
    public PropertyRange getRange(Filter filter) {
        if (!(filter instanceof And)) return null;

        And classBreakFilter = (And) filter;
        List<org.opengis.filter.Filter> children = classBreakFilter.getChildren();

        if (children == null || children.size() != 2) return null;

        org.opengis.filter.Filter child1 = children.get(0);
        if (!(child1 instanceof PropertyIsGreaterThanOrEqualTo || child1 instanceof PropertyIsGreaterThan))
            return null;
        BinaryComparisonOperator lowerBound = (BinaryComparisonOperator) child1;

        org.opengis.filter.Filter child2 = children.get(1);
        if (!(child2 instanceof PropertyIsLessThanOrEqualTo || child2 instanceof PropertyIsLessThan))
            return null;
        BinaryComparisonOperator upperBound = (BinaryComparisonOperator) child2;
        Expression property1 = lowerBound.getExpression1();
        Expression property2 = upperBound.getExpression1();

        if (property1 == null || property2 == null || !(property1.equals(property2))) {
            return null;
        }
        if (!(property1 instanceof PropertyName)) {
            return null;
        }
        String propertyName = ((PropertyName) property1).getPropertyName();

        Expression min = lowerBound.getExpression2();
        if (!(min instanceof Literal)) {
            return null;
        }
        Double minAsDouble = min.evaluate(null, double.class);

        Expression max = upperBound.getExpression2();
        if (!(max instanceof Literal)) {
            return null;
        }
        Double maxAsDouble = max.evaluate(null, double.class);

        return new PropertyRange(propertyName, new NumberRange(Double.class, minAsDouble,
                maxAsDouble));
    }
}
