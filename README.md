# Elm Packages

- [Elm Packages](https://package.elm-lang.org/)
- [Elm Packages Source Code](https://github.com/elm/package.elm-lang.org/)

## Extracting All Elm Packages

Run the following script on `https://package.elm-lang.org`.

```javascript
$$('.pkg-summary').map(m => ({
    name: m.querySelector('h1 > a').innerText,
    link: m.querySelector('h1 > a').getAttribute('href'),
    desc: m.querySelector('.pkg-summary-desc').innerHTML,
    version: m.querySelector('.pkg-summary-hints').lastChild.innerText,
}))
```

## Generate `all-packages.json` file

```sh
$ deno run --allow-write packages.ts
```
