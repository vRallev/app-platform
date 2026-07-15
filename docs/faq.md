# FAQ

#### How can I incrementally adopt App Platform?

App Platform offers many recommendations and best practices and hardly enforces any principles, e.g.
it’s possible to adopt the concept of the module structure without the `Scope` class or `Presenters`.
`Presenters` can be used without Compose UI. This and the fact that App Platform is extensible allows
for an incremental adoption. Apps can leverage the concepts and the framework without migrating all code at
once.

For example, instead of going all in on the unidirectional dataflow, Android apps can start adopting `Presenters` and
`Renderers` on an Activity by Activity or Fragment by Fragment basis. Today we recommend starting
new App Platform code with Metro. Earlier, our Android app initially used
[Dagger 2](https://dagger.dev/) and [Anvil](https://github.com/square/anvil) as dependency
injection framework and later made it interop with `kotlin-inject-anvil` before switching fully.


#### Can I use [Dagger 2](https://dagger.dev/) or any other DI framework?

It depends, but likely yes. App Platform recommends [Metro](di.md) as the default DI framework because
it supports Kotlin Multiplatform, verifies the dependency graph at compile time, and is the direction
the framework docs and examples assume.

[kotlin-inject-anvil](https://github.com/amzn/kotlin-inject-anvil) remains supported as the
alternative, especially for existing codebases or when you need compatibility with older App Platform
examples.

Dagger 2 is more challenging, because it only supports Android and JVM application. Metro is the
recommended default today, though App Platform started on Android with Dagger 2 and we first
bridged those Dagger 2 components with `kotlin-inject-anvil` for interop.


#### How does App Platform compare to [Circuit](https://slackhq.github.io/circuit/)?

Circuit shares certain aspects with App Platform in regards to implementing the unidirectional dataflow,
e.g. presenters and decoupling UI. How `Screens` with Circuit work vs how App Platform relies on composing presenters
and renderers is different.

App Platform goes further and has feature that Circuit doesn't provide, e.g. the module structure, the strong
emphasis on fakes and robots.

App Platform was built at Amazon months before Circuit was released in 2022. At that point there was no reason for
the applications using App Platform to migrate to Circuit.

!!! note "Help needed"

    Help from the community for a more in-depth comparison is needed.


#### Is App Platform used in production by Amazon?

App Platform was developed within the Amazon Delivery organization and is used to share code between several
applications and platforms. Public products include the [in-vehicle delivery app](https://www.youtube.com/watch?v=0T_zvUEqsD4),
[Amazon Flex for Android and iOS](https://flex.amazon.com/), the Linux based
[Vision-Assisted Package Retrieval](https://www.aboutamazon.com/news/transportation/amazon-vapr-delivery-van-packages)
and [Smart Glasses](https://www.aboutamazon.com/news/transportation/smart-glasses-amazon-delivery-drivers).
