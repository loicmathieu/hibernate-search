/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.JavaClassPojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoCaster;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class JavaBeanTypeModel<T> implements PojoRawTypeModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;
	private final PojoRawTypeIdentifier<T> typeIdentifier;
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;
	private final PojoCaster<T> caster;
	private final XClass xClass;
	private final Map<String, XProperty> declaredProperties;

	JavaBeanTypeModel(JavaBeanBootstrapIntrospector introspector, PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		this.introspector = introspector;
		this.typeIdentifier = typeIdentifier;
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
		this.caster = new JavaClassPojoCaster<>( typeIdentifier.getJavaClass() );
		this.xClass = introspector.toXClass( typeIdentifier.getJavaClass() );
		this.declaredProperties = introspector.getDeclaredMethodAccessXPropertiesByName( xClass );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JavaBeanTypeModel<?> that = (JavaBeanTypeModel<?>) o;
		/*
		 * We need to take the introspector into account, so that the engine does not confuse
		 * type models from different mappers during bootstrap.
		 */
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( typeIdentifier, that.typeIdentifier );
	}

	@Override
	public int hashCode() {
		return Objects.hash( introspector, typeIdentifier );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + typeIdentifier + "]";
	}

	@Override
	public PojoRawTypeIdentifier<T> getTypeIdentifier() {
		return typeIdentifier;
	}

	@Override
	public String getName() {
		return typeIdentifier.toString();
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract( typeIdentifier.getJavaClass().getModifiers() );
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel other) {
		return other instanceof JavaBeanTypeModel
				&& ( (JavaBeanTypeModel<?>) other ).typeIdentifier.getJavaClass().isAssignableFrom( typeIdentifier.getJavaClass() );
	}

	@Override
	public PojoRawTypeModel<? super T> getRawType() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<JavaBeanTypeModel<? super T>> getAscendingSuperTypes() {
		return (Stream<JavaBeanTypeModel<? super T>>) introspector.getAscendingSuperTypes( xClass );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<? extends PojoRawTypeModel<? super T>> getDescendingSuperTypes() {
		return (Stream<? extends PojoRawTypeModel<? super T>>) introspector.getDescendingSuperTypes( xClass );
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return introspector.getAnnotations( xClass );
	}

	@Override
	public PojoPropertyModel<?> getProperty(String propertyName) {
		return getAscendingSuperTypes()
				.map( model -> model.declaredProperties.get( propertyName ) )
				.filter( Objects::nonNull )
				.findFirst().map( this::createProperty )
				.orElseThrow( () -> log.cannotFindProperty( this, propertyName ) );
	}

	@Override
	public Stream<PojoPropertyModel<?>> getDeclaredProperties() {
		return declaredProperties.values().stream()
				.map( this::createProperty );
	}

	@Override
	public PojoCaster<T> getCaster() {
		return caster;
	}

	RawTypeDeclaringContext<T> getRawTypeDeclaringContext() {
		return rawTypeDeclaringContext;
	}

	private PojoPropertyModel<?> createProperty(XProperty property) {
		return new JavaBeanPropertyModel<>( introspector, this, property );
	}
}
