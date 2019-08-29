
1. Functional validation

```java
validate(greaterThan(arg1, 0), notEmpty(arg2))
  .then(arg1, arg2 -> {
    return Response.ok(doStuff(arg1, arg2));
  })
  .orElse(e -> {
    // Note, Validation is applicative functor - if both fails, 
    // both validation errors will be returned
    return Response.badRequest(e);
  }
```

2. Imperative preconditions

```java
try {
  greaterThan(arg1, 0);
  notEmpty(arg2)
  return Response.ok(doStuff());
} catch (ValidationException e) {
  // Note, need to handle ValidationException only 
  // and make sure nested code does not trow it
  return Response.badRequest(e);
}
```

Or we can configure special Spring error handler,
but again we may mix in validation errors which we do not expect thrown from nested code

```java
greaterThan(arg1, 0);
notEmpty(arg2)
return Response.ok(doStuff());
```

3. Try

```java
Try.product(greaterThan(arg1, 0), notEmpty(arg1, 0)).match(
  tuple -> {
    return Response.ok(doStuff(tuple._1, tuple._2));
  },
  e -> {
    // Note, Try is a monad, not applicative functor - if both fails, 
    // the first validation error will be returned
    return Response.badRequest(e);
  })
```

3. Reactor

```java
Mono.zip(greaterThan(arg1, 0), notEmpty(arg1, 0))
  .map(tuple -> {
    return Response.ok(doStuff(tuple._1, tuple._2));
  }).onErrorResume(e -> {
    return Response.badRequest(e);
  })
```
