# plum

## Installation

```sh
cd ~

git clone https://github.com/Lokeh/plum .plum

# Install globally with

ln -s ~/.plum/plum /usr/local/bin/plum
    
#, or link to another folder on your path as `~/bin`.

# Needs to be run at least once to install the global aliases
plum update

# Usage
plum help
```

## Usage

Many of the `plum` commands are supported by community-built aliases. Type
`plum help` to find out where to get more information about them.

Below is documentation for the aliases that are developed as part of the the
`plum` project.


### plum.add

`plum add` (or `plum.add/add-deps`) adds a new or updates the version of a
dependency in a deps map of a local `deps.edn` file. It supports git deps, local
file roots and maven deps.

This command will preserve any additional information in your deps.edn including
exclusions, other dependencies, etc.

#### Examples

From the command line:

```
cd my-project

# maven dep
plum add clj-time "0.14.2"

# -> {:deps {clj-time {:mvn/version "0.14.2"}}}

# git dep
plum add https://github.com/yourname/time-lib.git "04d2744549214b5cba04002b6875bdf59f9d88b6"

# -> {:deps {yourname/time-lib {:git/url "https://github.com/yourname/time-lib.git" :sha ,,,}}}

# local root
plum add /path/to/time-lib

# -> {:deps {time-lib {:local/root "path/to/time-lib"}}}
```
