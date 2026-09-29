// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.ralf.app.platform.scope.Scoped

interface MissingQualifierSuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<MissingQualifierSuperType>(),
)<!>
@ContributesIntoSet(AppScope::class, binding = binding<Scoped>())
class MissingQualifier : MissingQualifierSuperType, Scoped

interface WrongQualifierSuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<WrongQualifierSuperType>(),
)<!>
@ContributesIntoSet(
  AppScope::class,
  binding = binding<@ForScope(Unit::class) Scoped>(),
)
class WrongQualifier : WrongQualifierSuperType, Scoped

interface WrongContributionScopeSuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<WrongContributionScopeSuperType>(),
)<!>
@ContributesIntoSet(
  Unit::class,
  binding = binding<@ForScope(AppScope::class) Scoped>(),
)
class WrongContributionScope : WrongContributionScopeSuperType, Scoped

interface WrongBindingTypeSuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<WrongBindingTypeSuperType>(),
)<!>
@ContributesIntoSet(
  AppScope::class,
  binding = binding<@ForScope(AppScope::class) WrongBindingTypeSuperType>(),
)
class WrongBindingType : WrongBindingTypeSuperType, Scoped

interface ClassQualifierSuperType

@ForScope(AppScope::class)
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<ClassQualifierSuperType>(),
)<!>
@ContributesIntoSet(AppScope::class, binding = binding<Scoped>())
class ClassQualifier : ClassQualifierSuperType, Scoped

interface NullableScopedSuperType

<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  AppScope::class,
  binding = binding<NullableScopedSuperType>(),
)<!>
@ContributesIntoSet(
  AppScope::class,
  binding = binding<@ForScope(AppScope::class) Scoped?>(),
)
class NullableScoped : NullableScopedSuperType, Scoped

interface FirstScopeSuperType

interface SecondScopeSuperType

@ContributesBinding(AppScope::class, binding = binding<FirstScopeSuperType>())
<!CONTRIBUTES_SCOPED_ERROR!>@ContributesBinding(
  Unit::class,
  binding = binding<SecondScopeSuperType>(),
)<!>
@ContributesIntoSet(
  AppScope::class,
  binding = binding<@ForScope(AppScope::class) Scoped>(),
)
class MissingSecondScope : FirstScopeSuperType, SecondScopeSuperType, Scoped
