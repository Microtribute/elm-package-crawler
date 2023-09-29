import allPackagesExtracted from './all-packages.json' assert { type: 'json' };
import elmJson from './elm.json' assert { type: 'json' }

type Package = {
    name: string;
    link: string;
    desc: string;
    version: string;
};

interface InstalledDependencies {
    direct: Record<string, string>
    indirect: Record<string, string>
}

const allPackages = allPackagesExtracted as Package[];

// const commands = allPackages
//     .map(p => p.name)
//     .map(n => `elm install ${n}`)
//     .join('\n')
// ;

const dependencies = elmJson.dependencies as InstalledDependencies; 

dependencies.direct = allPackages
    .reduce((a, c) => Object.assign(a, { [c.name]: c.version }), elmJson.dependencies.direct)
;

dependencies.indirect = {};

Deno.writeTextFile('./elm-copy.json', JSON.stringify(elmJson, null, 4));
