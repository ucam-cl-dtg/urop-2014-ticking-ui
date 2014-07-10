'use strict';

/* Controllers */

angular.module('eTick.controllers', []).controller('sideBarController',
		[ '$scope', function($scope) {

			$scope.rolesList = [ {
				name : 'Submission',
				groups : [ {
					name : 'Foo'
				}, {
					name : 'Bar'
				} ]
			}, {
				name : 'Review',
				groups : [ {
					name : 'Foo'
				} ]
			}, {
				name : 'Author',
				groups : [ {
					name : 'Bar'
				} ]
			}, {
				name : 'Overview',
				groups : [ {
					name : 'Foo'
				}, {
					name : 'Bar'
				} ]
			} ];
		} ]);
