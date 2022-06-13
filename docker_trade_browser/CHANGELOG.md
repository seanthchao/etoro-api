# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.1] - 2021-08-20
### Fixed
- added missing font

## [0.2.0] - 2021-02-16
### Added
- label "podman-run-x11" for `podman container runlabel`

### Fixed
- added `libgl1` to fix some browser crashes
  and to reduce ubiquitous "Aw, Snap!" errors ("Error code: 6")
- docker-compose: increase size of `/dev/shm` to fix some "Aw, Snap!" errors and video playback
  ("ERROR:broker_posix.cc(46)] Received unexpected number of handles")
- docker-compose: run "init" (e.g. `tini`) as pid `1` to safely kill child processes

## [0.1.0] - 2021-02-15
### Added
- brave browser installed from brave's apt repo
- docker-compose config

[Unreleased]: https://github.com/fphammerle/docker-brave-browser/compare/v0.2.1...HEAD
[0.2.1]: https://github.com/fphammerle/docker-brave-browser/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/fphammerle/docker-brave-browser/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/fphammerle/docker-brave-browser/releases/tag/v0.1.0
